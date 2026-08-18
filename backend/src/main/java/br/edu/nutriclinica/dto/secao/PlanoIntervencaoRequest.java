package br.edu.nutriclinica.dto.secao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import org.openapitools.jackson.nullable.JsonNullable;

import java.math.BigDecimal;

/**
 * Schema `PlanoIntervencao` do contrato, no papel de corpo de PATCH.
 *
 * <p>Os três percentuais de macronutriente não são validados como soma 100: o
 * plano é preenchido campo a campo durante a consulta, e exigir o fechamento a
 * cada digitação impediria o estagiário de salvar o que já escreveu. Cada um é
 * validado só na própria faixa.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlanoIntervencaoRequest(
        JsonNullable<String> objetivos,

        JsonNullable<@Min(value = 0, message = "A prescrição energética não pode ser negativa.")
                     Integer> prescricaoEnergeticaKcal,

        JsonNullable<@DecimalMin(value = "0", message = "O percentual de carboidrato não pode ser negativo.")
                     @DecimalMax(value = "100", message = "O percentual de carboidrato deve ser de no máximo 100.")
                     BigDecimal> percCarboidrato,

        JsonNullable<@DecimalMin(value = "0", message = "O percentual de proteína não pode ser negativo.")
                     @DecimalMax(value = "100", message = "O percentual de proteína deve ser de no máximo 100.")
                     BigDecimal> percProteina,

        JsonNullable<@DecimalMin(value = "0", message = "O percentual de lipídeo não pode ser negativo.")
                     @DecimalMax(value = "100", message = "O percentual de lipídeo deve ser de no máximo 100.")
                     BigDecimal> percLipideo,

        JsonNullable<String> estrategiasComportamentais,
        JsonNullable<String> educacaoAlimentar) {
}
