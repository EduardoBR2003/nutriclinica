package br.edu.nutriclinica.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Schema `TermoConsentimentoRequest` do contrato.
 *
 * <p>{@code aceiteLgpd} é {@code Boolean} e não {@code boolean} de propósito: com
 * o primitivo, um corpo que omitisse o campo viraria {@code false} silencioso e
 * o {@code @NotNull} nunca dispararia. Registrar recusa por omissão não é o
 * mesmo que registrar recusa.
 *
 * <p>{@code registradoPor} não está aqui: quem registrou é o usuário
 * autenticado, não algo que o cliente escolhe.
 */
public record TermoConsentimentoRequest(

        @NotNull(message = "O aceite da LGPD é obrigatório.")
        Boolean aceiteLgpd,

        Boolean autorizaUsoPesquisa,

        @NotNull(message = "A data do aceite é obrigatória.")
        @PastOrPresent(message = "A data do aceite não pode estar no futuro.")
        LocalDate dataAceite,

        @Size(max = 2000, message = "As observações devem ter no máximo 2000 caracteres.")
        String observacoes) {

    /** Ausente equivale a não autorizar: o uso em pesquisa exige opt-in explícito. */
    public boolean autorizaUsoPesquisaOuFalso() {
        return Boolean.TRUE.equals(autorizaUsoPesquisa);
    }
}
