package br.edu.nutriclinica.dto.secao;

import br.edu.nutriclinica.domain.enums.Frequencia;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.openapitools.jackson.nullable.JsonNullable;

/** Schema `FrequenciaAlimentar` do contrato, no papel de corpo de PATCH. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FrequenciaAlimentarRequest(
        JsonNullable<Frequencia> frutas,
        JsonNullable<Frequencia> verdurasLegumes,
        JsonNullable<Frequencia> ultraprocessados,
        JsonNullable<Frequencia> refrigerante,
        JsonNullable<Frequencia> bebidaAlcoolica,
        JsonNullable<Frequencia> cafe,

        JsonNullable<@Min(value = 0, message = "A ingestão de água não pode ser negativa.")
                     @Max(value = 10000, message = "A ingestão de água deve ser de no máximo 10000 ml por dia.")
                     Integer> aguaMlDia,

        JsonNullable<String> observacoes) {
}
