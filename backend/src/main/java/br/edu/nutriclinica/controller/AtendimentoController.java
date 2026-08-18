package br.edu.nutriclinica.controller;

import br.edu.nutriclinica.domain.enums.StatusAtendimento;
import br.edu.nutriclinica.dto.AtendimentoCompletoResponse;
import br.edu.nutriclinica.dto.AtendimentoRequest;
import br.edu.nutriclinica.dto.AtendimentoResponse;
import br.edu.nutriclinica.dto.PaginaResponse;
import br.edu.nutriclinica.service.AtendimentoService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Abertura e leitura de atendimentos.
 *
 * <p>Quem vê o quê é decidido na query do serviço, por perfil. Abrir atendimento
 * é exclusivo do ESTAGIARIO: o atendimento nasce em nome de quem está
 * autenticado, e o vínculo exigido é o dele com o supervisor informado.
 */
@RestController
@RequestMapping("/api/atendimentos")
@Tag(name = "Atendimentos")
public class AtendimentoController {

    private static final int TAMANHO_PADRAO = 20;
    private static final int TAMANHO_MAXIMO = 100;

    private final AtendimentoService atendimentoService;

    public AtendimentoController(AtendimentoService atendimentoService) {
        this.atendimentoService = atendimentoService;
    }

    @GetMapping
    @Operation(summary = "Lista atendimentos conforme o perfil do usuário",
            description = "Estagiário vê apenas os próprios. Supervisor vê os dos seus orientados. "
                    + "Admin vê todos.")
    public ResponseEntity<PaginaResponse<AtendimentoResponse>> listar(
            @RequestParam(required = false) StatusAtendimento status,
            @RequestParam(required = false) Long pacienteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Mais recentes primeiro: é a ordem da fila de trabalho na tela.
        PageRequest paginacao = PageRequest.of(
                Math.max(page, 0), tamanho(size), Sort.by(Sort.Direction.DESC, "dataConsulta", "id"));

        return ResponseEntity.ok(atendimentoService.listar(status, pacienteId, paginacao));
    }

    @PostMapping
    @PreAuthorize("hasRole('ESTAGIARIO')")
    @Operation(summary = "Abre um novo atendimento",
            description = "Falha com 409 SEM_TERMO_CONSENTIMENTO se o paciente não tiver "
                    + "termo LGPD registrado.")
    public ResponseEntity<AtendimentoResponse> criar(@Valid @RequestBody AtendimentoRequest requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(atendimentoService.criar(requisicao));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retorna o prontuário completo")
    public ResponseEntity<AtendimentoCompletoResponse> detalhar(@PathVariable Long id) {
        return ResponseEntity.ok(atendimentoService.detalhar(id));
    }

    private int tamanho(int size) {
        return size < 1 ? TAMANHO_PADRAO : Math.min(size, TAMANHO_MAXIMO);
    }
}
