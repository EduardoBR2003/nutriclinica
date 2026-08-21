package br.edu.nutriclinica.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Schema `TermoConsentimentoRequest` do contrato.
 *
 * <p>{@code aceiteLgpd} é {@code Boolean} e não {@code boolean} de propósito: com
 * o primitivo, um corpo que omitisse o campo viraria {@code false} silencioso e
 * o {@code @NotNull} nunca dispararia. Registrar recusa por omissão não é o
 * mesmo que registrar recusa.
 *
 * <p>{@code dataAceite} não está aqui, e {@code registradoPor} tampouco: quando
 * e por quem o consentimento foi registrado é o registro de auditoria da LGPD, e
 * um registro que o próprio cliente escolhe não prova nada. O servidor carimba a
 * data do dia no primeiro registro e não a reescreve depois.
 */
public record TermoConsentimentoRequest(

        @NotNull(message = "O aceite da LGPD é obrigatório.")
        Boolean aceiteLgpd,

        Boolean autorizaUsoPesquisa,

        @Size(max = 2000, message = "As observações devem ter no máximo 2000 caracteres.")
        String observacoes) {

    /** Ausente equivale a não autorizar: o uso em pesquisa exige opt-in explícito. */
    public boolean autorizaUsoPesquisaOuFalso() {
        return Boolean.TRUE.equals(autorizaUsoPesquisa);
    }
}
