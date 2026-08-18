package br.edu.nutriclinica.dto.secao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.openapitools.jackson.nullable.JsonNullable;

/** Schema `DiagnosticoPes` do contrato — Problema, Etiologia, Sinais/Sintomas. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DiagnosticoPesRequest(
        JsonNullable<String> problema,
        JsonNullable<String> etiologia,
        JsonNullable<String> sinaisSintomas) {
}
