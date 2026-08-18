package br.edu.nutriclinica.dto.secao;

import br.edu.nutriclinica.domain.RecordatorioItem;
import br.edu.nutriclinica.domain.RecordatorioRefeicao;
import br.edu.nutriclinica.domain.enums.TipoRefeicao;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/** Schema `Refeicao` do contrato, no papel de resposta. */
public record RefeicaoResponse(
        Long id,
        TipoRefeicao tipoRefeicao,
        @JsonFormat(pattern = "HH:mm") LocalTime horario,
        String localRefeicao,
        Integer ordem,
        List<ItemRefeicaoResponse> itens) {

    public static RefeicaoResponse de(RecordatorioRefeicao refeicao) {
        return new RefeicaoResponse(
                refeicao.getId(),
                refeicao.getTipoRefeicao(),
                refeicao.getHorario(),
                refeicao.getLocalRefeicao(),
                refeicao.getOrdem(),
                refeicao.getItens().stream()
                        .sorted(Comparator.comparingInt(RecordatorioItem::getOrdem))
                        .map(ItemRefeicaoResponse::de)
                        .toList());
    }

    public record ItemRefeicaoResponse(
            Long id,
            String alimento,
            String quantidade,
            String medidaCaseira,
            Integer ordem) {

        public static ItemRefeicaoResponse de(RecordatorioItem item) {
            return new ItemRefeicaoResponse(
                    item.getId(),
                    item.getAlimento(),
                    item.getQuantidade(),
                    item.getMedidaCaseira(),
                    item.getOrdem());
        }
    }
}
