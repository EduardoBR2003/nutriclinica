package br.edu.nutriclinica.dto.secao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Schema `ExameBioquimico` do contrato, no papel de item de um PUT de coleção. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExameBioquimicoRequest(
        Long id,

        @NotBlank(message = "O nome do exame é obrigatório.")
        @Size(max = 80, message = "O nome do exame deve ter no máximo 80 caracteres.")
        String nomeExame,

        BigDecimal valor,

        @Size(max = 20, message = "A unidade deve ter no máximo 20 caracteres.")
        String unidade,

        LocalDate dataExame,

        @Size(max = 60, message = "O valor de referência deve ter no máximo 60 caracteres.")
        String valorReferencia) {
}
