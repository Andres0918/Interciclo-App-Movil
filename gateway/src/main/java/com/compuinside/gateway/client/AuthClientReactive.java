package com.compuinside.gateway.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuthClientReactive {
    private final WebClient.Builder webClientBuilder;

    public Mono<Boolean> isTokenValid(String token) {
        return webClientBuilder.build()
                .get()
                .uri("lb://auth/internal/tokens/check?token={token}", token)
                .retrieve()
                .bodyToMono(Boolean.class);
    }
}


