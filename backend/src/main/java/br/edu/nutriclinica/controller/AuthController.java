package br.edu.nutriclinica.controller;

import br.edu.nutriclinica.dto.CadastroRequest;
import br.edu.nutriclinica.dto.LoginRequest;
import br.edu.nutriclinica.dto.RefreshRequest;
import br.edu.nutriclinica.dto.TokenResponse;
import br.edu.nutriclinica.dto.UsuarioResponse;
import br.edu.nutriclinica.service.AuthService;
import br.edu.nutriclinica.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    private final UsuarioService usuarioService;

    public AuthController(AuthService authService, UsuarioService usuarioService) {
        this.authService = authService;
        this.usuarioService = usuarioService;
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

    /**
     * Auto-cadastro da tela de entrada. Fica junto do login, e não em
     * /api/usuarios, porque é a única escrita de usuário que acontece sem token —
     * deixá-la sob o controller do ADMIN exigiria abrir um buraco na regra de
     * classe daquele recurso.
     *
     * <p>Responde o usuário criado, não tokens: a conta nasce inativa e ainda não
     * tem direito a sessão nenhuma.
     */
    @PostMapping("/cadastro")
    @SecurityRequirements
    @Operation(summary = "Auto-cadastro de estagiário ou supervisor")
    public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody CadastroRequest requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.autoCadastrar(requisicao));
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do usuário autenticado")
    public ResponseEntity<UsuarioResponse> me() {
        return ResponseEntity.ok(authService.usuarioAutenticado());
    }
}
