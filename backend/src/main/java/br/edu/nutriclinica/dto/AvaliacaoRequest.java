package br.edu.nutriclinica.dto;

import br.edu.nutriclinica.domain.enums.ResultadoAvaliacao;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Schema `AvaliacaoRequest` do contrato: o parecer do supervisor e a rubrica.
 *
 * <p>Repare no que <b>não</b> existe aqui: {@code notaFinal}. É o mesmo
 * mecanismo que protege os indicadores da antropometria — o campo simplesmente
 * não tem onde pousar na desserialização, então uma nota forjada no corpo morre
 * antes de chegar ao serviço. Não há {@code if} descartando valor do cliente
 * porque não há valor do cliente a descartar: quem calcula é o
 * {@code AvaliacaoService}, sempre, a partir dos itens.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AvaliacaoRequest(

        @NotNull(message = "O resultado da avaliação é obrigatório.")
        ResultadoAvaliacao resultado,

        String parecerGeral,

        @NotEmpty(message = "A rubrica precisa de ao menos um critério.")
        List<@Valid Item> itens) {

    /**
     * Um critério da rubrica.
     *
     * <p>O {@code peso} é opcional e vale 1 quando ausente, como o
     * {@code default: 1} do contrato e o {@code DEFAULT 1} da coluna. Exigi-lo
     * estritamente positivo é o que torna a média ponderada bem definida: com
     * {@code nota} em [0,10] e pesos positivos, a nota final está provadamente em
     * [0,10] e satisfaz {@code ck_avaliacao_nota} por construção.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(

            @NotBlank(message = "O critério é obrigatório.")
            @Size(max = 120, message = "O critério deve ter no máximo 120 caracteres.")
            String criterio,

            @DecimalMin(value = "0", inclusive = false, message = "O peso deve ser maior que zero.")
            @DecimalMax(value = "99.99", message = "O peso deve ser no máximo 99,99.")
            BigDecimal peso,

            @NotNull(message = "A nota é obrigatória.")
            @DecimalMin(value = "0", message = "A nota mínima é 0.")
            @DecimalMax(value = "10", message = "A nota máxima é 10.")
            BigDecimal nota,

            String comentario) {
    }
}
