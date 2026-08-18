package br.edu.nutriclinica.dto.secao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

/**
 * Schema `QueixaPrincipal` do contrato, no papel de corpo de PATCH.
 *
 * <p>Cada campo é {@code JsonNullable} porque as duas situações precisam ser
 * distinguíveis: a chave que não veio mantém o que está gravado, a chave enviada
 * como {@code null} limpa o campo. O formulário salva por campo enquanto o
 * estagiário digita, então tratar ausente como {@code null} apagaria o resto da
 * seção a cada digitação.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record QueixaPrincipalRequest(
        JsonNullable<String> motivo,
        JsonNullable<@Size(max = 100, message = "O tempo de queixa deve ter no máximo 100 caracteres.") String> tempoQueixa,
        JsonNullable<String> tratamentoAnterior,
        JsonNullable<String> objetivoConsulta,
        JsonNullable<String> observacoes) {
}
