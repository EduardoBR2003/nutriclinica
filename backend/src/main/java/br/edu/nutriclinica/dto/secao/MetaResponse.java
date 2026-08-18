package br.edu.nutriclinica.dto.secao;

import br.edu.nutriclinica.domain.Meta;
import br.edu.nutriclinica.domain.enums.PrazoMeta;

import java.time.LocalDate;

/** Schema `Meta` do contrato, no papel de resposta. */
public record MetaResponse(
        Long id,
        String descricao,
        PrazoMeta prazo,
        String indicador,
        LocalDate dataRetorno) {

    public static MetaResponse de(Meta meta) {
        return new MetaResponse(
                meta.getId(),
                meta.getDescricao(),
                meta.getPrazo(),
                meta.getIndicador(),
                meta.getDataRetorno());
    }
}
