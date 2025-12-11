package com.compuinside.gateway.jwt;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class FirebaseAuthFilter extends AbstractGatewayFilterFactory<FirebaseAuthFilter.Config> {

    @Autowired
    private FirebaseAuth firebaseAuth;

    @Autowired
    private RouteValidator routeValidator;

    public FirebaseAuthFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            log.info(" Petición recibida en Gateway: {}", exchange.getRequest().getPath());

            // Permitir OPTIONS (CORS preflight)
            if (exchange.getRequest().getMethod().name().equalsIgnoreCase("OPTIONS")) {
                log.info(" OPTIONS detectado, permitiendo sin filtro");
                return chain.filter(exchange);
            }

            // Verificar si la ruta requiere autenticación
            if (routeValidator.isSecured.test(exchange.getRequest())) {
                log.info("🔒 Ruta segura detectada: {}", exchange.getRequest().getPath());

                String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    log.warn("❌ Falta el token de autenticación");
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                String token = authHeader.substring(7);

                // Validar token de Firebase de forma reactiva
                return validateFirebaseToken(token)
                        .flatMap(firebaseToken -> {
                            String uid = firebaseToken.getUid();
                            String email = firebaseToken.getEmail();
                            String name = firebaseToken.getName();

                            // Extraer claims personalizados si existen
                            String role = (String) firebaseToken.getClaims().getOrDefault("role", "USER");
                            String accountId = (String) firebaseToken.getClaims().getOrDefault("accountId", "");

                            log.info("✅ Token válido para usuario: {}", email);
                            log.info("👤 UID: {}", uid);
                            log.info("👤 Role: {}", role);
                            log.info("👤 Account ID: {}", accountId);

                            // Agregar información del usuario a los headers del request
                            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                    .header("X-User-UID", uid)
                                    .header("X-User-Email", email)
                                    .header("X-User-Name", name != null ? name : "")
                                    .header("X-User-Role", role)
                                    .header("X-Account-Id", accountId)
                                    .build();

                            ServerWebExchange mutatedExchange = exchange.mutate()
                                    .request(mutatedRequest)
                                    .build();

                            // Validar permisos de ruta según rol
                            return validateRoutePermissions(mutatedExchange, role)
                                    .then(chain.filter(mutatedExchange));
                        })
                        .onErrorResume(e -> {
                            log.error("❌ Error validando token: {}", e.getMessage());
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        });
            }

            // Ruta no protegida, continuar sin validación
            log.info("🔓 Ruta pública, continuando sin autenticación");
            return chain.filter(exchange);
        };
    }


    private Mono<FirebaseToken> validateFirebaseToken(String token) {
        return Mono.fromCallable(() -> {
            try {
                return firebaseAuth.verifyIdToken(token);
            } catch (FirebaseAuthException e) {
                throw new RuntimeException("Token inválido o expirado: " + e.getMessage());
            }
        });
    }

    private Mono<Void> validateRoutePermissions(ServerWebExchange exchange, String role) {
        String path = exchange.getRequest().getPath().toString();

        // Ejemplo: Solo ADMIN puede acceder a rutas /admin/*
        if (path.startsWith("/admin/") && !role.equals("ADMIN")) {
            log.warn(" Acceso denegado: Usuario con rol {} intentó acceder a {}", role, path);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        // Ejemplo: Solo MODERATOR o ADMIN pueden eliminar publicaciones
        if (path.matches(".*/publicacion/.*/delete") &&
                !role.equals("ADMIN") && !role.equals("MODERATOR")) {
            log.warn(" Acceso denegado: Rol {} no puede eliminar publicaciones", role);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        return Mono.empty();
    }

    public static class Config {
        // Configuración adicional si es necesaria
    }
}