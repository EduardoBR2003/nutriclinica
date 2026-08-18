package br.edu.nutriclinica.dto;

import br.edu.nutriclinica.domain.enums.SecaoProntuario;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Corpo do POST de comentário. O autor é sempre o usuário autenticado. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ComentarioSecaoRequest(

        @NotNull(message = "A seção comentada é obrigatória.")
        SecaoProntuario secao,

        @NotBlank(message = "O texto do comentário é obrigatório.")
        String texto) {
}
