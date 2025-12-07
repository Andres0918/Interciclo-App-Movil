package com.compuinside.auth.controller;

import com.compuinside.auth.jwt.FirebaseAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/empresa")
@RequiredArgsConstructor
public class EmpresaController {

    private final FirebaseAuthService authService;

}
