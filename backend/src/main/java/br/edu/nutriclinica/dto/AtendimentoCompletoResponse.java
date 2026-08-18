package br.edu.nutriclinica.dto;

import br.edu.nutriclinica.domain.enums.SecaoProntuario;
import br.edu.nutriclinica.domain.enums.StatusAtendimento;
import br.edu.nutriclinica.dto.secao.ComportamentoAlimentarResponse;
import br.edu.nutriclinica.dto.secao.DiagnosticoPesResponse;
import br.edu.nutriclinica.dto.secao.ExameBioquimicoResponse;
import br.edu.nutriclinica.dto.secao.FrequenciaAlimentarResponse;
import br.edu.nutriclinica.dto.secao.HistoriaClinicaResponse;
import br.edu.nutriclinica.dto.secao.MedicamentoResponse;
import br.edu.nutriclinica.dto.secao.MetaResponse;
import br.edu.nutriclinica.dto.secao.PlanoIntervencaoResponse;
import br.edu.nutriclinica.dto.secao.QueixaPrincipalResponse;
import br.edu.nutriclinica.dto.secao.RefeicaoResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Schema `AtendimentoCompleto` do contrato: o prontuário inteiro numa resposta.
 *
 * <p>O contrato o define como `Atendimento` mais as seções, todas opcionais.
 * Campos nulos são omitidos do JSON, então uma seção ainda não preenchida
 * simplesmente não aparece — que é como o contrato a descreve. Por isso coleção
 * vazia entra aqui como {@code null} e não como {@code []}: um recordatório sem
 * refeições e um recordatório não preenchido são o mesmo estado, e a tela lê
 * essa ausência para saber o que ainda falta.
 *
 * <p>{@code avaliacao} não é seção do prontuário, é o veredito sobre ele — por
 * isso entra ao lado das seções, e não dentro delas. Fica ausente enquanto o
 * supervisor não avaliou, e some do JSON pela mesma regra dos campos nulos.
 *
 * <p>Os campos base são repetidos em vez de aninhados porque o contrato os põe
 * no nível de cima (`allOf`). A montagem parte de um {@link AtendimentoResponse}
 * já pronto, para que exista uma única regra de como um atendimento vira JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AtendimentoCompletoResponse(
        Long id,
        String numeroProntuario,
        PacienteResponse paciente,
        UsuarioResponse estagiario,
        UsuarioResponse supervisor,
        LocalDate dataConsulta,
        StatusAtendimento status,
        boolean editavel,
        List<SecaoProntuario> secoesPreenchidas,
        LocalDateTime submetidoEm,
        LocalDateTime avaliadoEm,

        QueixaPrincipalResponse queixaPrincipal,
        HistoriaClinicaResponse historiaClinica,
        List<MedicamentoResponse> medicamentos,
        AntropometriaResponse antropometria,
        List<ExameBioquimicoResponse> exames,
        List<RefeicaoResponse> recordatorio,
        FrequenciaAlimentarResponse frequenciaAlimentar,
        ComportamentoAlimentarResponse comportamentoAlimentar,
        DiagnosticoPesResponse diagnostico,
        PlanoIntervencaoResponse plano,
        List<MetaResponse> metas,
        AvaliacaoResponse avaliacao) {

    /** As onze seções, na ordem em que o contrato e o formulário as apresentam. */
    public record Secoes(
            QueixaPrincipalResponse queixaPrincipal,
            HistoriaClinicaResponse historiaClinica,
            List<MedicamentoResponse> medicamentos,
            AntropometriaResponse antropometria,
            List<ExameBioquimicoResponse> exames,
            List<RefeicaoResponse> recordatorio,
            FrequenciaAlimentarResponse frequenciaAlimentar,
            ComportamentoAlimentarResponse comportamentoAlimentar,
            DiagnosticoPesResponse diagnostico,
            PlanoIntervencaoResponse plano,
            List<MetaResponse> metas) {
    }

    public static AtendimentoCompletoResponse de(AtendimentoResponse base,
                                                 Secoes secoes,
                                                 AvaliacaoResponse avaliacao) {
        return new AtendimentoCompletoResponse(
                base.id(),
                base.numeroProntuario(),
                base.paciente(),
                base.estagiario(),
                base.supervisor(),
                base.dataConsulta(),
                base.status(),
                base.editavel(),
                base.secoesPreenchidas(),
                base.submetidoEm(),
                base.avaliadoEm(),
                secoes.queixaPrincipal(),
                secoes.historiaClinica(),
                ouNulo(secoes.medicamentos()),
                secoes.antropometria(),
                ouNulo(secoes.exames()),
                ouNulo(secoes.recordatorio()),
                secoes.frequenciaAlimentar(),
                secoes.comportamentoAlimentar(),
                secoes.diagnostico(),
                secoes.plano(),
                ouNulo(secoes.metas()),
                avaliacao);
    }

    /** Lista vazia é seção não preenchida, e seção não preenchida não vai ao JSON. */
    private static <T> List<T> ouNulo(List<T> itens) {
        return itens == null || itens.isEmpty() ? null : itens;
    }
}
