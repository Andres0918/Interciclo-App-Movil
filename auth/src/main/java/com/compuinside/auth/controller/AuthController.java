package com.compuinside.auth.controller;


import com.compuinside.auth.jwt.FirebaseAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final FirebaseAuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /auth/register - Email: {}", request.getEmail());
        return authService.register(request);
    }

    @PostMapping("/login")
    public Mono<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /auth/login - Email: {}", request.getEmail());
        return authService.login(request);
    }

    @PostMapping("/verify")
    public Mono<AuthResponse> verifyToken(@Valid @RequestBody VerifyTokenRequest request) {
        log.info("POST /auth/verify");
        return authService.verifyToken(request.getToken());
    }

    @GetMapping("/profile/{uid}")
    public Mono<UserProfile> getProfile(@PathVariable String uid) {
        log.info("GET /auth/profile/{}", uid);
        return authService.getUserProfile(uid)
                .switchIfEmpty(Mono.error(new RuntimeException("Usuario no encontrado")));
    }

    @PutMapping("/profile/{uid}")
    public Mono<AuthResponse> updateProfile(
            @PathVariable String uid,
            @RequestBody Map<String, Object> updates) {
        log.info("PUT /auth/profile/{}", uid);
        return authService.updateUserProfile(uid, updates);
    }

    @DeleteMapping("/user/{uid}")
    public Mono<AuthResponse> deleteUser(@PathVariable String uid) {
        log.info("DELETE /auth/user/{}", uid);
        return authService.deleteUser(uid);
    }

    @PostMapping("/user/{uid}/disable")
    public Mono<AuthResponse> disableUser(@PathVariable String uid) {
        log.info("POST /auth/user/{}/disable", uid);
        return authService.disableUser(uid);
    }

    @PostMapping("/user/{uid}/enable")
    public Mono<AuthResponse> enableUser(@PathVariable String uid) {
        log.info("POST /auth/user/{}/enable", uid);
        return authService.enableUser(uid);
    }

    @PostMapping("/send-verification")
    public Mono<AuthResponse> sendEmailVerification(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        log.info("POST /auth/send-verification - Email: {}", email);
        return authService.sendEmailVerification(email);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "service", "auth-service",
                "firebase", "connected"
        );
    }
}
