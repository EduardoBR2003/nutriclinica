package br.edu.nutriclinica.dto.secao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.openapitools.jackson.nullable.JsonNullable;

/**
 * Schema `ComportamentoAlimentar` do contrato, no papel de corpo de PATCH.
 *
 * <p>São todos booleanos de três estados: {@code true}, {@code false} e "ainda
 * não perguntei". O {@code JsonNullable} é o que permite ao estagiário voltar a
 * seção para "não perguntei" enviando {@code null}, em vez de ficar preso ao
 * {@code false} que marcou por engano.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ComportamentoAlimentarRequest(
        JsonNullable<Boolean> comeAssistindoTela,
        JsonNullable<Boolean> comeRapido,
        JsonNullable<Boolean> pulaRefeicoes,
        JsonNullable<Boolean> compulsaoAlimentar,
        JsonNullable<Boolean> alimentacaoEmocional,
        JsonNullable<Boolean> restricaoAlimentar,
        JsonNullable<String> observacoes) {
}
