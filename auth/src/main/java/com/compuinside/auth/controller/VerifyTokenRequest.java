package com.compuinside.auth.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyTokenRequest {

    @NotBlank(message = "Token es requerido")
    private String token;
}
