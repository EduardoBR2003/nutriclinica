package br.edu.nutriclinica.service.secao;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.dto.AtendimentoCompletoResponse;
import br.edu.nutriclinica.service.AntropometriaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Junta as onze seções de um atendimento num objeto só, para o
 * {@code GET /api/atendimentos/{id}}.
 *
 * <p>Recebe o atendimento já carregado com o escopo aplicado: quem chama é o
 * {@code AtendimentoService}, que passou pelo validador. Nenhuma leitura daqui
 * refaz essa checagem, e nenhuma delas deve ser exposta direto num controller.
 *
 * <p>O custo é fixo — onze consultas, independentemente de quantas seções estão
 * preenchidas, quantas refeições o recordatório tem ou quantos itens cada uma.
 * A do recordatório traz os itens por {@code join fetch}; as demais são de linha
 * única ou de lista simples, sem consulta aninhada.
 */
@Service
public class ProntuarioCompletoService {

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

    public ProntuarioCompletoService(QueixaPrincipalService queixaPrincipalService,
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

    @Transactional(readOnly = true)
    public AtendimentoCompletoResponse.Secoes secoesDe(Atendimento atendimento) {
        Long id = atendimento.getId();

        return new AtendimentoCompletoResponse.Secoes(
                queixaPrincipalService.buscarDoAtendimento(id).orElse(null),
                historiaClinicaService.buscarDoAtendimento(id).orElse(null),
                medicamentoService.doAtendimento(id),
                // A antropometria precisa do atendimento inteiro, não só do id:
                // os derivados dependem do sexo do paciente e da idade dele na
                // data da consulta.
                antropometriaService.buscarDoAtendimento(atendimento).orElse(null),
                exameBioquimicoService.doAtendimento(id),
                recordatorioService.doAtendimento(id),
                frequenciaAlimentarService.buscarDoAtendimento(id).orElse(null),
                comportamentoAlimentarService.buscarDoAtendimento(id).orElse(null),
                diagnosticoPesService.buscarDoAtendimento(id).orElse(null),
                planoIntervencaoService.buscarDoAtendimento(id).orElse(null),
                metaService.doAtendimento(id));
    }
}
