package br.edu.nutriclinica.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Schema `Pagina` do contrato.
 *
 * <p>Existe para que o {@code PageImpl} do Spring Data não seja serializado
 * direto: o JSON dele carrega estrutura interna (`pageable`, `sort`, `first`,
 * `numberOfElements`…) que não está no contrato e que muda de formato entre
 * versões do Spring — o frontend passaria a depender de detalhe de framework.
 *
 * @param content lista já convertida para o DTO de resposta
 */
public record PaginaResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int number,
        int size) {

    /**
     * Monta a página a partir do conteúdo já convertido.
     *
     * <p>Recebe a lista pronta em vez de um conversor item a item porque os
     * campos derivados (possuiTermo, secoesPreenchidas) são levantados em lote
     * para a página inteira — converter aqui, um a um, traria de volta o N+1.
     */
    public static <E, T> PaginaResponse<T> de(Page<E> pagina, List<T> conteudoConvertido) {
        return new PaginaResponse<>(
                conteudoConvertido,
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.getNumber(),
                pagina.getSize());
    }
}
