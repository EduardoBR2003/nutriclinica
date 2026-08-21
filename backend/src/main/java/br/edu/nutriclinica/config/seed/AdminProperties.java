package br.edu.nutriclinica.config.seed;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Credenciais do administrador inicial, prefixo {@code app.admin}, vindas de
 * ADMIN_EMAIL e ADMIN_PASSWORD.
 *
 * <p>O registro deste bean acontece só no perfil prod (ver
 * {@link AdminInicialRunner}), e as constraints abaixo são o que faz a
 * aplicação <b>recusar subir</b> ali sem as variáveis: a validação roda na
 * ligação das propriedades, antes de qualquer requisição ser aceita. Um
 * ambiente de produção sem administrador é um ambiente em que ninguém cadastra
 * ninguém — falhar alto na partida é melhor do que descobrir isso depois do
 * deploy.
 *
 * <p>O mínimo de 12 caracteres é maior que o do cadastro comum (8) de
 * propósito: esta é a única conta que existe antes de qualquer outra, e ela
 * cria todas as demais.
 */
@ConfigurationProperties(prefix = "app.admin")
@Validated
public record AdminProperties(

        @NotBlank(message = "Defina ADMIN_EMAIL: sem ela não há administrador inicial em produção.")
        @Email(message = "ADMIN_EMAIL não é um e-mail válido.")
        String email,

        @NotBlank(message = "Defina ADMIN_PASSWORD: sem ela não há administrador inicial em produção.")
        @Size(min = 12, message = "ADMIN_PASSWORD deve ter no mínimo 12 caracteres.")
        String senha) {
}
