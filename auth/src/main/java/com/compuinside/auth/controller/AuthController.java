package com.compuinside.auth.controller;

import com.compuinside.auth.jwt.JwtService;
import com.compuinside.auth.jwt.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/register-user")
    public ResponseEntity<AuthResponse> registerwithAccount(@RequestBody AccountRequest registerRequest) {
        return ResponseEntity.ok(authService.createUserWithExistingAccount(registerRequest));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerwithoutAccount(@RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.ok(authService.createUserWithNewAccount(registerRequest));
    }

    @GetMapping("/getAppointments")
    public ResponseEntity<String> getAppointmentsByDoctor(
            @RequestHeader("userId") UUID doctorId, @RequestHeader("accountId") UUID accountId
    ) {
        System.out.println("UID: " + doctorId); // 👈 sigue saliendo en consola
        return ResponseEntity.ok("UID Usuario: " + doctorId + "- UID Account: " + accountId); // 👈 se devuelve como respuesta HTTP
    }

    @GetMapping("/hello")
    public String hello() {
        return "holaaa";
    }

    /*@GetMapping("/validate")
    public String validateToken(@RequestParam("token") String token) {
        try {
            authService.validateToken(token);
            return "Token is valid";
        } catch (Exception e) {
            return "Token is invalid";
        }

    }*/

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(authService.refreshToken(token));
    }
}
