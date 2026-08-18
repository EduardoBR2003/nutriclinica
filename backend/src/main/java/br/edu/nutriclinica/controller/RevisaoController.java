package br.edu.nutriclinica.controller;

import br.edu.nutriclinica.dto.AtendimentoResponse;
import br.edu.nutriclinica.dto.AvaliacaoRequest;
import br.edu.nutriclinica.dto.AvaliacaoResponse;
import br.edu.nutriclinica.dto.ComentarioSecaoRequest;
import br.edu.nutriclinica.dto.ComentarioSecaoResponse;
import br.edu.nutriclinica.dto.PaginaResponse;
import br.edu.nutriclinica.service.AvaliacaoService;
import br.edu.nutriclinica.service.ComentarioSecaoService;
import br.edu.nutriclinica.service.RevisaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * O lado do supervisor: fila de revisão, avaliação e comentários.
 *
 * <p>Sem {@code @RequestMapping} de classe porque o contrato põe estes endpoints
 * em duas raízes — {@code /api/revisoes} e {@code /api/atendimentos/{id}} —, e a
 * tag {@code Revisão} é o que os agrupa. Também não poderiam morar no
 * {@code SecaoProntuarioController}, que compartilha o prefixo mas é privativo
 * do ESTAGIARIO por {@code @PreAuthorize} de classe.
 *
 * <p>O {@code hasRole} barra o perfil errado já na borda; que o supervisor seja
 * <b>o designado</b> para aquele atendimento é decidido no serviço de domínio,
 * onde a regra vale para qualquer chamador. Os GET não têm {@code hasRole}: quem
 * enxerga o atendimento lê o parecer, e é assim que o estagiário vê a própria
 * nota.
 */
@RestController
@Tag(name = "Revisão")
@Validated
public class RevisaoController {

    private static final int TAMANHO_PADRAO = 20;
    private static final int TAMANHO_MAXIMO = 100;

    private final RevisaoService revisaoService;
    private final AvaliacaoService avaliacaoService;
    private final ComentarioSecaoService comentarioSecaoService;

    public RevisaoController(RevisaoService revisaoService,
                             AvaliacaoService avaliacaoService,
                             ComentarioSecaoService comentarioSecaoService) {
        this.revisaoService = revisaoService;
        this.avaliacaoService = avaliacaoService;
        this.comentarioSecaoService = comentarioSecaoService;
    }

    @GetMapping("/api/revisoes/pendentes")
    @PreAuthorize("hasRole('SUPERVISOR')")
    @Operation(summary = "Fila de atendimentos aguardando revisão do supervisor",
            description = "Os EM_REVISAO em que o usuário autenticado é o supervisor designado, "
                    + "do mais antigo para o mais recente.")
    public ResponseEntity<PaginaResponse<AtendimentoResponse>> pendentes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Mais antigo primeiro: quem esperou mais é revisado antes. O id desempata
        // submissões do mesmo instante, para a paginação ser estável.
        PageRequest paginacao = PageRequest.of(
                Math.max(page, 0), tamanho(size), Sort.by(Sort.Direction.ASC, "submetidoEm", "id"));

        return ResponseEntity.ok(revisaoService.pendentes(paginacao));
    }

    @GetMapping("/api/atendimentos/{id}/avaliacao")
    @Operation(summary = "Consulta a avaliação do atendimento")
    public ResponseEntity<AvaliacaoResponse> avaliacao(@PathVariable Long id) {
        return ResponseEntity.ok(avaliacaoService.buscar(id));
    }

    @PostMapping("/api/atendimentos/{id}/avaliacao")
    @PreAuthorize("hasRole('SUPERVISOR')")
    @Operation(summary = "Registra a avaliação e conclui a revisão",
            description = "Exige status EM_REVISAO e que o usuário seja o supervisor designado. "
                    + "A nota final é calculada pelo servidor como média ponderada da rubrica; "
                    + "um `notaFinal` enviado no corpo é ignorado.")
    public ResponseEntity<AvaliacaoResponse> avaliar(@PathVariable Long id,
                                                     @Valid @RequestBody AvaliacaoRequest requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(avaliacaoService.registrar(id, requisicao));
    }

    @GetMapping("/api/atendimentos/{id}/comentarios")
    @Operation(summary = "Lista comentários por seção")
    public ResponseEntity<List<ComentarioSecaoResponse>> comentarios(@PathVariable Long id) {
        return ResponseEntity.ok(comentarioSecaoService.listar(id));
    }

    @PostMapping("/api/atendimentos/{id}/comentarios")
    @PreAuthorize("hasRole('SUPERVISOR')")
    @Operation(summary = "Adiciona comentário a uma seção")
    public ResponseEntity<ComentarioSecaoResponse> comentar(
            @PathVariable Long id, @Valid @RequestBody ComentarioSecaoRequest requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(comentarioSecaoService.adicionar(id, requisicao));
    }

    private int tamanho(int size) {
        return size < 1 ? TAMANHO_PADRAO : Math.min(size, TAMANHO_MAXIMO);
    }
}
