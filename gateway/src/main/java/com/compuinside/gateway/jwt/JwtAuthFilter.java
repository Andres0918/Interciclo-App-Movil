package com.compuinside.gateway.jwt;

import com.compuinside.gateway.service.TokenValidationService;
import com.compuinside.gateway.util.JwtUtil;
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

import java.util.UUID;

@Component
@Slf4j
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RouteValidator routeValidator;

    private final TokenValidationService authClient;

    public JwtAuthFilter(TokenValidationService authClient) {
        super(Config.class);
        this.authClient = authClient;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {

            log.info(" Petición recibida en Gateway: " + exchange.getRequest().getPath());

            if (exchange.getRequest().getMethod().name().equalsIgnoreCase("OPTIONS")) {
                log.info("OPTIONS detectado, permitiendo sin filtro ");
                return chain.filter(exchange);
            }

            if (routeValidator.isSecured.test(exchange.getRequest())) {
                log.info("🔒 Ruta segura detectada: " + exchange.getRequest().getPath());

                String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    log.info("❌ Falta el token JWT");
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                String token = authHeader.substring(7);
                if (!jwtUtil.validateToken(token)) {
                    log.info("❌ Token inválido");
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
                return authClient.isTokenValid(token)
                        .flatMap(isValid -> {
                            if (!isValid) {
                                log.info("❌ Token inválido, expirado o revocado en Auth-Service");
                                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                                return exchange.getResponse().setComplete();
                            }

                            log.info("✅ Token válido: " + token);

                            String username = jwtUtil.getUsernameFromToken(token);
                            String role = jwtUtil.getRoleFromToken(token);
                            String module = jwtUtil.getModuleFromToken(token);
                            UUID userId = jwtUtil.getUserIdFromToken(token);
                            UUID accountId = jwtUtil.getAccountIdFromToken(token);
                            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                    .header("loggedInUser", username)
                                    .header("role", role)
                                    .header("module", module)
                                    .header("userId", userId.toString())
                                    .header("accountId", accountId.toString())
                                    .build();
                            ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

                            log.info("👤 Usuario autenticado: " + username);
                            log.info("👤 Rol del usuario: " + role);
                            log.info("👤 Módulo del usuario: " + module);
                            log.info("👤 ID del usuario: " + userId);
                            log.info("👤 ID de la cuenta: " + accountId);

                            // 🔒 Verificar a qué microservicio pertenece y si tiene permiso
                            return validateRoute(mutatedExchange, module)
                                    .then(Mono.defer(() -> {
                                        // Continuar con la cadena de filtros si la validación es exitosa
                                        return chain.filter(mutatedExchange)
                                                .doOnSuccess(aVoid -> {
                                                    if (!exchange.getResponse().isCommitted()) {
                                                        log.info("📝 Modificando la respuesta...");
                                                        exchange.getResponse().getHeaders().add("loggedInUser", username);
                                                        exchange.getResponse().getHeaders().add("role", role);
                                                        exchange.getResponse().getHeaders().add("module", module);
                                                        log.info("👤 Encabezados de la respuesta: " + exchange.getResponse().getHeaders());
                                                    } else {
                                                        log.info("⚠️ La respuesta ya ha sido enviada, no se puede modificar.");
                                                    }
                                                });
                                    }));
                        });
            }

            // Continuar con el request original si la ruta no es protegida
            return chain.filter(exchange);
        };
    }

    public static class Config {
        // Por ahora no tocar xd
    }

    private Mono<Void> validateRoute(ServerWebExchange exchange, String module) {
        if (exchange.getRequest().getPath().toString().startsWith("/med/") && !module.equals("DOCTOR_MODULE")) {
            log.info("🚫 Acceso denegado: Usuario no pertenece al módulo médico.");
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }
        return Mono.empty();
    }
}
