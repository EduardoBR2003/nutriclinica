package br.edu.nutriclinica.dto;

import br.edu.nutriclinica.domain.AvaliacaoSupervisor;
import br.edu.nutriclinica.domain.ItemRubrica;
import br.edu.nutriclinica.domain.enums.ResultadoAvaliacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Schema `Avaliacao` do contrato.
 *
 * <p>{@code notaFinal} é somente leitura: o servidor a calcula como média
 * ponderada da rubrica e o cliente não tem como influenciá-la — o
 * {@link AvaliacaoRequest} nem sequer declara o campo.
 *
 * <p>Só pode ser montado <b>dentro</b> da transação: {@code supervisor} e
 * {@code itens} são LAZY na entidade, e com {@code open-in-view: false} tocá-los
 * depois viraria {@code LazyInitializationException} na serialização.
 */
public record AvaliacaoResponse(
        Long id,
        UsuarioResponse supervisor,
        BigDecimal notaFinal,
        ResultadoAvaliacao resultado,
        String parecerGeral,
        LocalDateTime avaliadoEm,
        List<Item> itens) {

    public record Item(
            Long id,
            String criterio,
            BigDecimal peso,
            BigDecimal nota,
            String comentario) {

        public static Item de(ItemRubrica item) {
            return new Item(
                    item.getId(),
                    item.getCriterio(),
                    item.getPeso(),
                    item.getNota(),
                    item.getComentario());
        }
    }

    public static AvaliacaoResponse de(AvaliacaoSupervisor avaliacao) {
        return new AvaliacaoResponse(
                avaliacao.getId(),
                UsuarioResponse.de(avaliacao.getSupervisor()),
                avaliacao.getNotaFinal(),
                avaliacao.getResultado(),
                avaliacao.getParecerGeral(),
                avaliacao.getAvaliadoEm(),
                avaliacao.getItens().stream().map(Item::de).toList());
    }
}
