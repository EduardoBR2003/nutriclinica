package br.edu.nutriclinica.controller;

import br.edu.nutriclinica.domain.enums.Perfil;
import br.edu.nutriclinica.dto.PaginaResponse;
import br.edu.nutriclinica.dto.UsuarioRequest;
import br.edu.nutriclinica.dto.UsuarioResponse;
import br.edu.nutriclinica.dto.VinculosRequest;
import br.edu.nutriclinica.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Gestão de usuários e vínculos — a área do ADMIN.
 *
 * <p>Tudo aqui é ADMIN, com uma exceção deliberada: {@code /supervisores}, que é
 * do ESTAGIARIO. Ele não lista usuários, lista os supervisores que já foram
 * vinculados a ele — o mesmo conjunto que a abertura de atendimento aceita, e
 * nada além disso.
 */
@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuários")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    /** Mesmos limites do contrato: page 0, size 20. */
    private static final int TAMANHO_PADRAO = 20;
    private static final int TAMANHO_MAXIMO = 100;

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @Operation(summary = "Lista usuários")
    public ResponseEntity<PaginaResponse<UsuarioResponse>> listar(
            @RequestParam(required = false) Perfil perfil,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(usuarioService.listar(
                perfil, PageRequest.of(Math.max(page, 0), tamanho(size), Sort.by("nome"))));
    }

    @PostMapping
    @Operation(summary = "Cria usuário")
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody UsuarioRequest requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.criar(requisicao));
    }

    /**
     * Precede o {@code /{id}} de propósito na leitura do arquivo, embora o Spring
     * já prefira a rota literal à variável de caminho.
     */
    @GetMapping("/supervisores")
    @PreAuthorize("hasRole('ESTAGIARIO')")
    @Operation(summary = "Supervisores que orientam o estagiário autenticado")
    public ResponseEntity<List<UsuarioResponse>> supervisoresDisponiveis() {
        return ResponseEntity.ok(usuarioService.supervisoresDisponiveis());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha usuário")
    public ResponseEntity<UsuarioResponse> detalhar(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.detalhar(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza usuário")
    public ResponseEntity<UsuarioResponse> atualizar(@PathVariable Long id,
                                                     @Valid @RequestBody UsuarioRequest requisicao) {
        return ResponseEntity.ok(usuarioService.atualizar(id, requisicao));
    }

    @GetMapping("/{id}/vinculos")
    @Operation(summary = "Estagiários hoje orientados por um supervisor")
    public ResponseEntity<List<UsuarioResponse>> vinculos(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.estagiariosVinculados(id));
    }

    @PutMapping("/{id}/vinculos")
    @Operation(summary = "Define os estagiários orientados por um supervisor")
    public ResponseEntity<Void> definirVinculos(@PathVariable Long id,
                                                @Valid @RequestBody VinculosRequest requisicao) {
        usuarioService.definirVinculos(id, requisicao.idsOuVazio());
        return ResponseEntity.noContent().build();
    }

    /** Teto no tamanho da página: sem ele, `size=100000` viraria um dump da base. */
    private int tamanho(int size) {
        return size < 1 ? TAMANHO_PADRAO : Math.min(size, TAMANHO_MAXIMO);
    }
}
