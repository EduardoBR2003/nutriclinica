package br.edu.nutriclinica.controller;

import br.edu.nutriclinica.dto.AntropometriaRequest;
import br.edu.nutriclinica.dto.AntropometriaResponse;
import br.edu.nutriclinica.dto.secao.ComportamentoAlimentarRequest;
import br.edu.nutriclinica.dto.secao.ComportamentoAlimentarResponse;
import br.edu.nutriclinica.dto.secao.DiagnosticoPesRequest;
import br.edu.nutriclinica.dto.secao.DiagnosticoPesResponse;
import br.edu.nutriclinica.dto.secao.ExameBioquimicoRequest;
import br.edu.nutriclinica.dto.secao.ExameBioquimicoResponse;
import br.edu.nutriclinica.dto.secao.FrequenciaAlimentarRequest;
import br.edu.nutriclinica.dto.secao.FrequenciaAlimentarResponse;
import br.edu.nutriclinica.dto.secao.HistoriaClinicaRequest;
import br.edu.nutriclinica.dto.secao.HistoriaClinicaResponse;
import br.edu.nutriclinica.dto.secao.MedicamentoRequest;
import br.edu.nutriclinica.dto.secao.MedicamentoResponse;
import br.edu.nutriclinica.dto.secao.MetaRequest;
import br.edu.nutriclinica.dto.secao.MetaResponse;
import br.edu.nutriclinica.dto.secao.PlanoIntervencaoRequest;
import br.edu.nutriclinica.dto.secao.PlanoIntervencaoResponse;
import br.edu.nutriclinica.dto.secao.QueixaPrincipalRequest;
import br.edu.nutriclinica.dto.secao.QueixaPrincipalResponse;
import br.edu.nutriclinica.dto.secao.RefeicaoRequest;
import br.edu.nutriclinica.dto.secao.RefeicaoResponse;
import br.edu.nutriclinica.service.AntropometriaService;
import br.edu.nutriclinica.service.secao.ComportamentoAlimentarService;
import br.edu.nutriclinica.service.secao.DiagnosticoPesService;
import br.edu.nutriclinica.service.secao.ExameBioquimicoService;
import br.edu.nutriclinica.service.secao.FrequenciaAlimentarService;
import br.edu.nutriclinica.service.secao.HistoriaClinicaService;
import br.edu.nutriclinica.service.secao.MedicamentoService;
import br.edu.nutriclinica.service.secao.MetaService;
import br.edu.nutriclinica.service.secao.PlanoIntervencaoService;
import br.edu.nutriclinica.service.secao.QueixaPrincipalService;
import br.edu.nutriclinica.service.secao.RecordatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * As onze seções do prontuário.
 *
 * <p>Duas formas, conforme a seção. As 1:1 são salvas por PATCH parcial: chave
 * ausente no corpo mantém, chave enviada como {@code null} limpa — é o que
 * permite ao formulário salvar campo a campo enquanto o estagiário digita. As
 * 1:N são substituídas inteiras por PUT.
 *
 * <p>O {@code hasRole} barra supervisor e admin já na borda. Que o estagiário
 * seja o dono do atendimento e que o status admita escrita é decidido no serviço
 * de domínio, pelo {@code AtendimentoEditavelValidator}, primeira linha de todos
 * os onze — a regra vale para qualquer chamador, não só para quem vem por aqui.
 *
 * <p>O {@code @Validated} na classe é o que faz o {@code @Valid} dos itens de
 * uma lista valer: sem ele, o Bean Validation não desce até os elementos de um
 * {@code List} recebido como corpo, e um medicamento sem nome passaria direto.
 */
@RestController
@RequestMapping("/api/atendimentos/{id}")
@Tag(name = "Seções do prontuário")
@Validated
@PreAuthorize("hasRole('ESTAGIARIO')")
public class SecaoProntuarioController {

    private final QueixaPrincipalService queixaPrincipalService;
    private final HistoriaClinicaService historiaClinicaService;
    private final AntropometriaService antropometriaService;
    private final FrequenciaAlimentarService frequenciaAlimentarService;
    private final ComportamentoAlimentarService comportamentoAlimentarService;
    private final DiagnosticoPesService diagnosticoPesService;
    private final PlanoIntervencaoService planoIntervencaoService;
    private final MedicamentoService medicamentoService;
    private final ExameBioquimicoService exameBioquimicoService;
    private final RecordatorioService recordatorioService;
    private final MetaService metaService;

    public SecaoProntuarioController(QueixaPrincipalService queixaPrincipalService,
                                     HistoriaClinicaService historiaClinicaService,
                                     AntropometriaService antropometriaService,
                                     FrequenciaAlimentarService frequenciaAlimentarService,
                                     ComportamentoAlimentarService comportamentoAlimentarService,
                                     DiagnosticoPesService diagnosticoPesService,
                                     PlanoIntervencaoService planoIntervencaoService,
                                     MedicamentoService medicamentoService,
                                     ExameBioquimicoService exameBioquimicoService,
                                     RecordatorioService recordatorioService,
                                     MetaService metaService) {
        this.queixaPrincipalService = queixaPrincipalService;
        this.historiaClinicaService = historiaClinicaService;
        this.antropometriaService = antropometriaService;
        this.frequenciaAlimentarService = frequenciaAlimentarService;
        this.comportamentoAlimentarService = comportamentoAlimentarService;
        this.diagnosticoPesService = diagnosticoPesService;
        this.planoIntervencaoService = planoIntervencaoService;
        this.medicamentoService = medicamentoService;
        this.exameBioquimicoService = exameBioquimicoService;
        this.recordatorioService = recordatorioService;
        this.metaService = metaService;
    }

    // ------------------------------------------------------------------
    // Seções 1:1 — PATCH parcial
    // ------------------------------------------------------------------

    @PatchMapping("/queixa-principal")
    @Operation(summary = "Salva a queixa principal")
    public ResponseEntity<QueixaPrincipalResponse> queixaPrincipal(
            @PathVariable Long id, @Valid @RequestBody QueixaPrincipalRequest requisicao) {
        return ResponseEntity.ok(queixaPrincipalService.salvar(id, requisicao));
    }

    @PatchMapping("/historia-clinica")
    @Operation(summary = "Salva a história clínica")
    public ResponseEntity<HistoriaClinicaResponse> historiaClinica(
            @PathVariable Long id, @Valid @RequestBody HistoriaClinicaRequest requisicao) {
        return ResponseEntity.ok(historiaClinicaService.salvar(id, requisicao));
    }

    @PatchMapping("/antropometria")
    @Operation(summary = "Salva a antropometria e retorna os indicadores calculados",
            description = "IMC, classificação e relação cintura/quadril são calculados pelo "
                    + "servidor. Campos calculados enviados pelo cliente são ignorados.")
    public ResponseEntity<AntropometriaResponse> antropometria(
            @PathVariable Long id, @Valid @RequestBody AntropometriaRequest requisicao) {
        return ResponseEntity.ok(antropometriaService.salvar(id, requisicao));
    }

    @PatchMapping("/frequencia-alimentar")
    @Operation(summary = "Salva a frequência alimentar e hidratação")
    public ResponseEntity<FrequenciaAlimentarResponse> frequenciaAlimentar(
            @PathVariable Long id, @Valid @RequestBody FrequenciaAlimentarRequest requisicao) {
        return ResponseEntity.ok(frequenciaAlimentarService.salvar(id, requisicao));
    }

    @PatchMapping("/comportamento-alimentar")
    @Operation(summary = "Salva o comportamento alimentar")
    public ResponseEntity<ComportamentoAlimentarResponse> comportamentoAlimentar(
            @PathVariable Long id, @Valid @RequestBody ComportamentoAlimentarRequest requisicao) {
        return ResponseEntity.ok(comportamentoAlimentarService.salvar(id, requisicao));
    }

    @PatchMapping("/diagnostico")
    @Operation(summary = "Salva o diagnóstico nutricional no modelo PES")
    public ResponseEntity<DiagnosticoPesResponse> diagnostico(
            @PathVariable Long id, @Valid @RequestBody DiagnosticoPesRequest requisicao) {
        return ResponseEntity.ok(diagnosticoPesService.salvar(id, requisicao));
    }

    @PatchMapping("/plano")
    @Operation(summary = "Salva o plano de intervenção nutricional")
    public ResponseEntity<PlanoIntervencaoResponse> plano(
            @PathVariable Long id, @Valid @RequestBody PlanoIntervencaoRequest requisicao) {
        return ResponseEntity.ok(planoIntervencaoService.salvar(id, requisicao));
    }

    // ------------------------------------------------------------------
    // Seções 1:N — PUT substitui a coleção inteira
    // ------------------------------------------------------------------

    @PutMapping("/medicamentos")
    @Operation(summary = "Substitui a lista de medicamentos e suplementos")
    public ResponseEntity<List<MedicamentoResponse>> medicamentos(
            @PathVariable Long id, @RequestBody List<@Valid MedicamentoRequest> requisicao) {
        return ResponseEntity.ok(medicamentoService.substituir(id, requisicao));
    }

    @PutMapping("/exames")
    @Operation(summary = "Substitui a lista de exames bioquímicos")
    public ResponseEntity<List<ExameBioquimicoResponse>> exames(
            @PathVariable Long id, @RequestBody List<@Valid ExameBioquimicoRequest> requisicao) {
        return ResponseEntity.ok(exameBioquimicoService.substituir(id, requisicao));
    }

    @PutMapping("/recordatorio")
    @Operation(summary = "Substitui o recordatório alimentar de 24 horas")
    public ResponseEntity<List<RefeicaoResponse>> recordatorio(
            @PathVariable Long id, @RequestBody List<@Valid RefeicaoRequest> requisicao) {
        return ResponseEntity.ok(recordatorioService.substituir(id, requisicao));
    }

    @PutMapping("/metas")
    @Operation(summary = "Substitui a lista de metas")
    public ResponseEntity<List<MetaResponse>> metas(
            @PathVariable Long id, @RequestBody List<@Valid MetaRequest> requisicao) {
        return ResponseEntity.ok(metaService.substituir(id, requisicao));
    }
}
