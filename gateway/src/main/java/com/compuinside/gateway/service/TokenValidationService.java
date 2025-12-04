package com.compuinside.gateway.service;

import com.compuinside.gateway.client.AuthClientReactive;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TokenValidationService {
    private final AuthClientReactive authClient;

    public Mono<Boolean> isTokenValid(String token) {
        return authClient.isTokenValid(token);
    }
}
