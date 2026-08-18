package br.edu.nutriclinica.controller;

import br.edu.nutriclinica.dto.LoginRequest;
import br.edu.nutriclinica.dto.RefreshRequest;
import br.edu.nutriclinica.dto.TokenResponse;
import br.edu.nutriclinica.dto.UsuarioResponse;
import br.edu.nutriclinica.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Autentica e retorna tokens")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest requisicao) {
        return ResponseEntity.ok(authService.login(requisicao));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Renova o access token")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest requisicao) {
        return ResponseEntity.ok(authService.refresh(requisicao));
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do usuário autenticado")
    public ResponseEntity<UsuarioResponse> me() {
        return ResponseEntity.ok(authService.usuarioAutenticado());
    }
}
