package com.compuinside.auth.controller;

import com.compuinside.auth.jwt.AuthService;
import com.compuinside.auth.user.Empresa;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/empresa")
@RequiredArgsConstructor
public class EmpresaController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Empresa> registerEmpresa(@RequestBody EmpresaRequest request) {
        Empresa empresa = authService.createEmpresa(request);
        return ResponseEntity.ok(empresa);
    }

    @GetMapping("/obtener")
    public List<Empresa> getAllEmpresas() {
        return authService.getAllEmpresas();
    }
}
