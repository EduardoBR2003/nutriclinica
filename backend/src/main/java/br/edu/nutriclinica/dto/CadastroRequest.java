package br.edu.nutriclinica.dto;

import br.edu.nutriclinica.domain.enums.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Schema `CadastroRequest` do contrato: o auto-cadastro da tela de entrada.
 *
 * <p>Não tem {@code ativo}: a conta nasce inativa e quem a ativa é o ADMIN. Se o
 * campo existisse aqui, bastaria mandá-lo {@code true} para pular a aprovação.
 *
 * <p>{@code perfil} é validado no serviço contra ESTAGIARIO e SUPERVISOR. O
 * enum inteiro é aceito na desserialização para que ADMIN vire um 422 explicando
 * a regra, e não um 400 de valor desconhecido.
 */
public record CadastroRequest(

        @NotNull(message = "O nome é obrigatório.")
        @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres.")
        String nome,

        @NotNull(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
        String email,

        @NotNull(message = "A senha é obrigatória.")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
        String senha,

        @NotNull(message = "O perfil é obrigatório.")
        Perfil perfil) {
}
