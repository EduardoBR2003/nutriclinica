package br.edu.nutriclinica.dto;

import br.edu.nutriclinica.domain.enums.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Schema `UsuarioRequest` do contrato — o cadastro manual, feito pelo ADMIN.
 *
 * <p>{@code senha} é opcional aqui porque o mesmo record serve à criação e à
 * atualização: no PUT, ausente ou em branco significa "mantenha a atual", e é a
 * única forma de editar um usuário sem que a tela precise reenviar a senha que
 * ela nunca recebeu. A obrigatoriedade na criação é do serviço, não da anotação
 * — Bean Validation não sabe qual das duas operações está em curso.
 *
 * <p>O teto de 72 caracteres não é estético: o BCrypt ignora tudo o que passa do
 * 72º byte, então uma senha mais longa daria a impressão de força que ela não
 * tem.
 */
public record UsuarioRequest(

        @NotNull(message = "O nome é obrigatório.")
        @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres.")
        String nome,

        @NotNull(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
        String email,

        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
        String senha,

        @NotNull(message = "O perfil é obrigatório.")
        Perfil perfil,

        Boolean ativo) {

    /** Ausente equivale a ativo: o ADMIN cadastra para que a pessoa entre. */
    public boolean ativoOuVerdadeiro() {
        return !Boolean.FALSE.equals(ativo);
    }

    /** Senha em branco é o mesmo que senha ausente: nenhuma troca foi pedida. */
    public boolean trocaSenha() {
        return senha != null && !senha.isBlank();
    }
}
