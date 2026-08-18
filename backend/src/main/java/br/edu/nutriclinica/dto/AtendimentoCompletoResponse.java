package br.edu.nutriclinica.dto;

import br.edu.nutriclinica.domain.enums.SecaoProntuario;
import br.edu.nutriclinica.domain.enums.StatusAtendimento;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Schema `AtendimentoCompleto` do contrato: o prontuário inteiro numa resposta.
 *
 * <p>O contrato o define como `Atendimento` mais as seções, todas opcionais.
 * Hoje só a antropometria tem endpoint; as outras dez entram no bloco de seções
 * do prontuário e ganham aqui um campo cada. Campos nulos são omitidos do JSON,
 * então uma seção ainda não preenchida simplesmente não aparece — que é como o
 * contrato a descreve.
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
        AntropometriaResponse antropometria) {

    public static AtendimentoCompletoResponse de(AtendimentoResponse base, AntropometriaResponse antropometria) {
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
                antropometria);
    }
}
