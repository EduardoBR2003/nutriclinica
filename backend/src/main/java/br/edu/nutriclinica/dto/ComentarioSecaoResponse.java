package br.edu.nutriclinica.dto;

import br.edu.nutriclinica.domain.ComentarioSecao;
import br.edu.nutriclinica.domain.enums.SecaoProntuario;

import java.time.LocalDateTime;

/**
 * Schema `ComentarioSecao` do contrato.
 *
 * <p>Como {@link AvaliacaoResponse}, só pode ser montado dentro da transação:
 * {@code autor} é LAZY na entidade.
 *
 * <p>{@code resolvido} nasce {@code false} e permanece: o contrato não tem
 * endpoint para virá-lo. Marcar pendência como resolvida é assunto de outro
 * bloco.
 */
public record ComentarioSecaoResponse(
        Long id,
        UsuarioResponse autor,
        SecaoProntuario secao,
        String texto,
        boolean resolvido,
        LocalDateTime criadoEm) {

    public static ComentarioSecaoResponse de(ComentarioSecao comentario) {
        return new ComentarioSecaoResponse(
                comentario.getId(),
                UsuarioResponse.de(comentario.getAutor()),
                comentario.getSecao(),
                comentario.getTexto(),
                comentario.isResolvido(),
                comentario.getCriadoEm());
    }
}
