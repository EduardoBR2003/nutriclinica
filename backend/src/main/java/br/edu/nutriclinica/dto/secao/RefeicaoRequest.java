package br.edu.nutriclinica.dto.secao;

import br.edu.nutriclinica.domain.enums.TipoRefeicao;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.List;

/**
 * Schema `Refeicao` do contrato: uma refeição do recordatório de 24 horas, com
 * os itens aninhados.
 *
 * <p>Refeições e itens chegam juntos e são gravados na mesma transação — um
 * recordatório com metade dos itens salvos não é um estado válido do prontuário.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RefeicaoRequest(
        Long id,

        @NotNull(message = "O tipo de refeição é obrigatório.")
        TipoRefeicao tipoRefeicao,

        // O contrato exemplifica "07:30"; sem o pattern, a resposta sairia como
        // "07:30:00" e deixaria de casar com o que o frontend envia.
        @JsonFormat(pattern = "HH:mm")
        LocalTime horario,

        @Size(max = 80, message = "O local da refeição deve ter no máximo 80 caracteres.")
        String localRefeicao,

        Integer ordem,

        List<@Valid ItemRefeicaoRequest> itens) {

    /** Um alimento dentro de uma refeição. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ItemRefeicaoRequest(
            Long id,

            @NotBlank(message = "O alimento é obrigatório.")
            @Size(max = 150, message = "O alimento deve ter no máximo 150 caracteres.")
            String alimento,

            @Size(max = 60, message = "A quantidade deve ter no máximo 60 caracteres.")
            String quantidade,

            @Size(max = 60, message = "A medida caseira deve ter no máximo 60 caracteres.")
            String medidaCaseira,

            Integer ordem) {
    }
}
