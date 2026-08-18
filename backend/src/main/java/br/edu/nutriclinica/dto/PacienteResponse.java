package br.edu.nutriclinica.dto;

import br.edu.nutriclinica.domain.Paciente;
import br.edu.nutriclinica.domain.enums.RacaCor;
import br.edu.nutriclinica.domain.enums.Sexo;

import java.time.LocalDate;
import java.time.Period;

/**
 * Schema `Paciente` do contrato. A entidade JPA nunca sai do controller.
 *
 * <p>{@code idade} e {@code possuiTermo} não são colunas: a primeira é calculada
 * da data de nascimento, a segunda vem da consulta em lote de termos com aceite.
 */
public record PacienteResponse(
        Long id,
        String nome,
        LocalDate dataNascimento,
        Integer idade,
        Sexo sexo,
        RacaCor racaCor,
        String telefone,
        String email,
        boolean possuiTermo) {

    public static PacienteResponse de(Paciente paciente, boolean possuiTermo) {
        return new PacienteResponse(
                paciente.getId(),
                paciente.getNome(),
                paciente.getDataNascimento(),
                idadeHoje(paciente.getDataNascimento()),
                paciente.getSexo(),
                paciente.getRacaCor(),
                paciente.getTelefone(),
                paciente.getEmail(),
                possuiTermo);
    }

    /**
     * Idade atual do paciente. Difere de propósito da idade usada na
     * antropometria, que é a da data da consulta: aqui o cadastro mostra quantos
     * anos a pessoa tem hoje.
     */
    private static Integer idadeHoje(LocalDate dataNascimento) {
        return dataNascimento == null ? null : Period.between(dataNascimento, LocalDate.now()).getYears();
    }
}
