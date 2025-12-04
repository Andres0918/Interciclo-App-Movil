package com.compuinside.auth.controller;

import com.compuinside.auth.jwt.JwtService;
import com.compuinside.auth.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class JWTController {
    private final TokenRepository tokenRepository;

    @GetMapping("/tokens/check")
    public boolean checkToken(@RequestParam String token){
        return tokenRepository.findByToken(token)
                .filter(t -> !t.isExpired() && !t.isRevoked())
                .isPresent();
    }
}
