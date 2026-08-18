package br.edu.nutriclinica.controller;

import br.edu.nutriclinica.dto.PacienteRequest;
import br.edu.nutriclinica.dto.PacienteResponse;
import br.edu.nutriclinica.dto.PaginaResponse;
import br.edu.nutriclinica.dto.PontoEvolucaoResponse;
import br.edu.nutriclinica.dto.TermoConsentimentoRequest;
import br.edu.nutriclinica.dto.TermoConsentimentoResponse;
import br.edu.nutriclinica.service.PacienteService;
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
 * Cadastro de pacientes e termo de consentimento.
 *
 * <p>A leitura é aberta a qualquer autenticado porque o que cada perfil enxerga
 * já é decidido na query do serviço — quem não tem o paciente no escopo recebe
 * lista vazia ou 404, sem precisar de regra na borda.
 *
 * <p>A escrita exige ESTAGIARIO ou SUPERVISOR: cadastrar paciente e registrar
 * consentimento é ato clínico, e o ADMIN gerencia usuários e vínculos.
 */
@RestController
@RequestMapping("/api/pacientes")
@Tag(name = "Pacientes")
public class PacienteController {

    /** Mesmos limites do contrato: page 0, size 20. */
    private static final int TAMANHO_PADRAO = 20;
    private static final int TAMANHO_MAXIMO = 100;

    private final PacienteService pacienteService;

    public PacienteController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @GetMapping
    @Operation(summary = "Lista pacientes visíveis ao usuário")
    public ResponseEntity<PaginaResponse<PacienteResponse>> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(pacienteService.listar(
                busca, PageRequest.of(Math.max(page, 0), tamanho(size), Sort.by("nome"))));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ESTAGIARIO','SUPERVISOR')")
    @Operation(summary = "Cadastra paciente")
    public ResponseEntity<PacienteResponse> criar(@Valid @RequestBody PacienteRequest requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pacienteService.criar(requisicao));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha paciente")
    public ResponseEntity<PacienteResponse> detalhar(@PathVariable Long id) {
        return ResponseEntity.ok(pacienteService.detalhar(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ESTAGIARIO','SUPERVISOR')")
    @Operation(summary = "Atualiza paciente")
    public ResponseEntity<PacienteResponse> atualizar(@PathVariable Long id,
                                                      @Valid @RequestBody PacienteRequest requisicao) {
        return ResponseEntity.ok(pacienteService.atualizar(id, requisicao));
    }

    @GetMapping("/{id}/termo")
    @Operation(summary = "Consulta o termo de consentimento")
    public ResponseEntity<TermoConsentimentoResponse> buscarTermo(@PathVariable Long id) {
        return ResponseEntity.ok(pacienteService.buscarTermo(id));
    }

    @PutMapping("/{id}/termo")
    @PreAuthorize("hasAnyRole('ESTAGIARIO','SUPERVISOR')")
    @Operation(summary = "Registra ou atualiza o termo de consentimento")
    public ResponseEntity<TermoConsentimentoResponse> salvarTermo(
            @PathVariable Long id,
            @Valid @RequestBody TermoConsentimentoRequest requisicao) {
        return ResponseEntity.ok(pacienteService.salvarTermo(id, requisicao));
    }

    @GetMapping("/{id}/evolucao")
    @Operation(summary = "Série histórica de peso, IMC e medidas para gráficos",
            description = "Monta a série com as antropometrias dos atendimentos APROVADOS "
                    + "do paciente, em ordem de data de consulta.")
    public ResponseEntity<List<PontoEvolucaoResponse>> evolucao(@PathVariable Long id) {
        return ResponseEntity.ok(pacienteService.evolucao(id));
    }

    /** Teto no tamanho da página: sem ele, `size=100000` viraria um dump da base. */
    private int tamanho(int size) {
        return size < 1 ? TAMANHO_PADRAO : Math.min(size, TAMANHO_MAXIMO);
    }
}
