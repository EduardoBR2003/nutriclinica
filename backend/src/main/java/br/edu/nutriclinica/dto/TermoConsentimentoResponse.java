package br.edu.nutriclinica.dto;

import br.edu.nutriclinica.domain.TermoConsentimento;

import java.time.LocalDate;

/**
 * Schema `TermoConsentimento` do contrato.
 *
 * <p>{@code registradoPor} é o nome de quem registrou, como no contrato — não o
 * id nem o objeto Usuario inteiro. Basta para a tela de auditoria do termo e
 * não expõe o e-mail de ninguém.
 */
public record TermoConsentimentoResponse(
        boolean aceiteLgpd,
        boolean autorizaUsoPesquisa,
        LocalDate dataAceite,
        String registradoPor,
        String observacoes) {

    public static TermoConsentimentoResponse de(TermoConsentimento termo) {
        return new TermoConsentimentoResponse(
                termo.isAceiteLgpd(),
                termo.isAutorizaUsoPesquisa(),
                termo.getDataAceite(),
                termo.getRegistradoPor().getNome(),
                termo.getObservacoes());
    }
}
