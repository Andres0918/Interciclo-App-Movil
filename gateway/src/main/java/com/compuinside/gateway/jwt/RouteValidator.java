package com.compuinside.gateway.jwt;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    private static final List<String> OPEN_ENDPOINTS = List.of(
            "/auth/login",
            "/auth/register",
            "/eureka"
    );

    public Predicate<ServerHttpRequest> isSecured = request -> {
        String path = request.getPath().toString();  // Obtener la ruta de forma consistente
        boolean isProtected = OPEN_ENDPOINTS.stream().noneMatch(path::startsWith);

        System.out.println("🔍 Revisando si " + path + " es una ruta protegida: " + isProtected);
        return isProtected;
    };
}
