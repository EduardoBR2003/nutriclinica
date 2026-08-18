package br.edu.nutriclinica.dto.secao;

import br.edu.nutriclinica.domain.enums.HabitoIntestinal;
import br.edu.nutriclinica.domain.enums.NivelEstresse;
import br.edu.nutriclinica.domain.enums.QualidadeSono;
import br.edu.nutriclinica.domain.enums.Tabagismo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.openapitools.jackson.nullable.JsonNullable;

import java.math.BigDecimal;

/** Schema `HistoriaClinica` do contrato, no papel de corpo de PATCH. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HistoriaClinicaRequest(
        JsonNullable<String> doencasDiagnosticadas,
        JsonNullable<String> alergias,
        JsonNullable<String> intolerancias,
        JsonNullable<String> historicoFamiliar,
        JsonNullable<Tabagismo> tabagismo,

        // A coluna é NUMERIC(3,1): sem o teto, 100 horas de sono viraria erro de
        // constraint do banco em vez de 422 no schema `Erro`.
        JsonNullable<@DecimalMin(value = "0", message = "As horas de sono não podem ser negativas.")
                     @DecimalMax(value = "24", message = "As horas de sono devem ser de no máximo 24.")
                     BigDecimal> horasSono,

        JsonNullable<QualidadeSono> qualidadeSono,
        JsonNullable<NivelEstresse> nivelEstresse,
        JsonNullable<HabitoIntestinal> habitoIntestinal,
        JsonNullable<Boolean> praticaAtividadeFisica,
        JsonNullable<String> descricaoAtividade,
        JsonNullable<String> observacoes) {
}
