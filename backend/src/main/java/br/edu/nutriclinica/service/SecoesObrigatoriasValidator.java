package br.edu.nutriclinica.service;

import br.edu.nutriclinica.domain.Antropometria;
import br.edu.nutriclinica.domain.DiagnosticoPes;
import br.edu.nutriclinica.domain.PlanoIntervencao;
import br.edu.nutriclinica.domain.QueixaPrincipal;
import br.edu.nutriclinica.dto.ErroResponse;
import br.edu.nutriclinica.repository.AntropometriaRepository;
import br.edu.nutriclinica.repository.DiagnosticoPesRepository;
import br.edu.nutriclinica.repository.PlanoIntervencaoRepository;
import br.edu.nutriclinica.repository.QueixaPrincipalRepository;
import br.edu.nutriclinica.repository.RecordatorioRefeicaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * O que precisa estar preenchido para um prontuário poder ser submetido.
 *
 * <p>Nem toda seção é obrigatória — história clínica, exames, medicamentos,
 * frequência, comportamento e metas enriquecem o atendimento, mas a ausência
 * delas é informação clínica legítima ("o paciente não usa medicamento") e não
 * pendência. Obrigatórias são as cinco que sustentam o raciocínio nutricional:
 * a queixa que motivou a consulta, a antropometria que a mede, o recordatório
 * que a documenta, o diagnóstico PES que a conclui e o plano que a responde.
 *
 * <p><b>Por que não reaproveitar {@link SecoesPreenchidasService}:</b> a query
 * nativa dele testa existência de linha, não conteúdo. O PATCH por seção grava
 * a linha mesmo quando todos os campos vêm nulos — é o que faz o autosave do
 * frontend funcionar —, então um diagnóstico vazio contaria como preenchido e o
 * portão da submissão cairia. Estender aquela query com predicados de conteúdo
 * mudaria o significado do {@code secoesPreenchidas} do contrato, que a tela usa
 * como checklist de "já mexi aqui", e espalharia política de domínio dentro de
 * SQL nativo. São coisas diferentes e ficam separadas.
 *
 * <p>Devolve a lista de pendências em vez de lançar exceção: o mesmo
 * levantamento serve para a tela antecipar o que falta antes de o estagiário
 * tentar submeter. Quem o transforma em 409 é o
 * {@link AtendimentoWorkflowService}.
 */
@Service
public class SecoesObrigatoriasValidator {

    private final QueixaPrincipalRepository queixaPrincipalRepository;
    private final AntropometriaRepository antropometriaRepository;
    private final RecordatorioRefeicaoRepository recordatorioRefeicaoRepository;
    private final DiagnosticoPesRepository diagnosticoPesRepository;
    private final PlanoIntervencaoRepository planoIntervencaoRepository;

    public SecoesObrigatoriasValidator(QueixaPrincipalRepository queixaPrincipalRepository,
                                       AntropometriaRepository antropometriaRepository,
                                       RecordatorioRefeicaoRepository recordatorioRefeicaoRepository,
                                       DiagnosticoPesRepository diagnosticoPesRepository,
                                       PlanoIntervencaoRepository planoIntervencaoRepository) {
        this.queixaPrincipalRepository = queixaPrincipalRepository;
        this.antropometriaRepository = antropometriaRepository;
        this.recordatorioRefeicaoRepository = recordatorioRefeicaoRepository;
        this.diagnosticoPesRepository = diagnosticoPesRepository;
        this.planoIntervencaoRepository = planoIntervencaoRepository;
    }

    /**
     * As pendências do prontuário, na ordem em que as seções aparecem na tela —
     * o frontend salta direto para a primeira.
     *
     * <p>Uma seção ausente por inteiro gera uma entrada para <b>cada</b> campo
     * obrigatório dela, e não uma entrada só da seção: quem preenche precisa
     * saber o que digitar, não só onde clicar. O caminho do campo segue os nomes
     * de propriedade do schema {@code AtendimentoCompleto}, que é a mesma
     * convenção dos 422 de validação — para o frontend existe uma regra só de
     * {@code campos[].campo} para controle do formulário.
     *
     * <p>As seções 1:1 são buscadas por {@code findById(atendimentoId)}: a chave
     * primária delas é o próprio id do atendimento ({@code @MapsId}).
     *
     * @return lista vazia quando o prontuário está pronto para submeter
     */
    @Transactional(readOnly = true)
    public List<ErroResponse.CampoErro> pendenciasDe(Long atendimentoId) {
        List<ErroResponse.CampoErro> pendencias = new ArrayList<>();

        QueixaPrincipal queixa = queixaPrincipalRepository.findById(atendimentoId).orElse(null);
        if (queixa == null || vazio(queixa.getMotivo())) {
            pendencias.add(pendencia("queixaPrincipal.motivo",
                    "Informe o motivo da consulta na queixa principal."));
        }

        Antropometria antropometria = antropometriaRepository.findById(atendimentoId).orElse(null);
        if (antropometria == null || antropometria.getPesoKg() == null) {
            pendencias.add(pendencia("antropometria.pesoKg", "Informe o peso aferido."));
        }
        if (antropometria == null || antropometria.getAlturaCm() == null) {
            pendencias.add(pendencia("antropometria.alturaCm", "Informe a altura aferida."));
        }

        if (!recordatorioRefeicaoRepository.existsByAtendimentoId(atendimentoId)) {
            // A exigência é sobre a coleção, não sobre um campo: por isso o
            // caminho não tem sufixo.
            pendencias.add(pendencia("recordatorio",
                    "Registre ao menos uma refeição no recordatório de 24 horas."));
        }

        DiagnosticoPes diagnostico = diagnosticoPesRepository.findById(atendimentoId).orElse(null);
        if (diagnostico == null || vazio(diagnostico.getProblema())) {
            pendencias.add(pendencia("diagnostico.problema",
                    "Informe o problema do diagnóstico PES."));
        }
        if (diagnostico == null || vazio(diagnostico.getEtiologia())) {
            pendencias.add(pendencia("diagnostico.etiologia",
                    "Informe a etiologia do diagnóstico PES."));
        }
        if (diagnostico == null || vazio(diagnostico.getSinaisSintomas())) {
            pendencias.add(pendencia("diagnostico.sinaisSintomas",
                    "Informe os sinais e sintomas do diagnóstico PES."));
        }

        PlanoIntervencao plano = planoIntervencaoRepository.findById(atendimentoId).orElse(null);
        if (plano == null || vazio(plano.getObjetivos())) {
            pendencias.add(pendencia("plano.objetivos",
                    "Informe os objetivos do plano de intervenção."));
        }

        return pendencias;
    }

    private ErroResponse.CampoErro pendencia(String campo, String mensagem) {
        return new ErroResponse.CampoErro(campo, mensagem);
    }

    /** Em branco conta como ausente: é o que sobra de um PATCH com texto apagado. */
    private boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }
}
