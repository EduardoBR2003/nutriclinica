package br.edu.nutriclinica.dto;

import br.edu.nutriclinica.domain.enums.RacaCor;
import br.edu.nutriclinica.domain.enums.Sexo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Schema `PacienteRequest` do contrato.
 *
 * <p>Não tem campo para {@code idade} nem {@code possuiTermo}: os dois são
 * derivados pelo servidor. A idade sai da data de nascimento e o termo da
 * existência de um aceite — aceitá-los do cliente seria guardar como fato algo
 * que o próprio servidor sabe calcular.
 *
 * <p>Os limites espelham as CHECKs de {@code ck_paciente_sexo} e
 * {@code ck_paciente_raca}: violação vira 422 no schema `Erro`, não erro de
 * constraint do banco.
 */
public record PacienteRequest(

        @NotNull(message = "O nome é obrigatório.")
        @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres.")
        String nome,

        @NotNull(message = "A data de nascimento é obrigatória.")
        @PastOrPresent(message = "A data de nascimento não pode estar no futuro.")
        LocalDate dataNascimento,

        @NotNull(message = "O sexo é obrigatório.")
        Sexo sexo,

        RacaCor racaCor,

        @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres.")
        String telefone,

        @Email(message = "E-mail inválido.")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
        String email) {
}
