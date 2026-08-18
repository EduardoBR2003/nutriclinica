package br.edu.nutriclinica.dto.secao;

import br.edu.nutriclinica.domain.enums.TipoMedicamento;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Schema `Medicamento` do contrato, no papel de item de um PUT de coleção.
 *
 * <p>Sem {@code JsonNullable}: o PUT substitui a lista inteira, então cada item
 * enviado é a versão final dele — não existe "campo ausente mantém o anterior".
 *
 * <p>O {@code id} identifica o registro que continua na lista. Item sem id é
 * novo; id que não pertence a esta coleção é tratado como desconhecido e vira
 * registro novo, nunca sequestra a linha de outro atendimento.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MedicamentoRequest(
        Long id,

        @NotNull(message = "O tipo é obrigatório.")
        TipoMedicamento tipo,

        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
        String nome,

        @Size(max = 60, message = "A dose deve ter no máximo 60 caracteres.")
        String dose,

        @Size(max = 60, message = "A frequência deve ter no máximo 60 caracteres.")
        String frequencia) {
}
