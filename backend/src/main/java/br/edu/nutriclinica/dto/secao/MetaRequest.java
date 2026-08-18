package br.edu.nutriclinica.dto.secao;

import br.edu.nutriclinica.domain.enums.PrazoMeta;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Schema `Meta` do contrato, no papel de item de um PUT de coleção. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MetaRequest(
        Long id,

        @NotBlank(message = "A descrição é obrigatória.")
        String descricao,

        @NotNull(message = "O prazo é obrigatório.")
        PrazoMeta prazo,

        @Size(max = 150, message = "O indicador deve ter no máximo 150 caracteres.")
        String indicador,

        LocalDate dataRetorno) {
}
