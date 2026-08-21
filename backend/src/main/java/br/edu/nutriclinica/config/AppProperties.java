package br.edu.nutriclinica.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Configuração da aplicação, prefixo `app`. O segredo do JWT e as origens de CORS
 * vêm das variáveis de ambiente JWT_SECRET e CORS_ALLOWED_ORIGINS — fora do perfil
 * dev não há valor padrão.
 *
 * <p>Não ter default <b>não basta</b> para a aplicação recusar subir sem elas: o
 * binder de {@code @ConfigurationProperties} ignora placeholder irresolúvel e
 * liga o texto cru, então {@code app.jwt.secret} chegaria aqui como a string
 * {@code "${JWT_SECRET}"} e a API subiria assinando token com um segredo que
 * está publicado neste arquivo. As constraints abaixo é que transformam a falta
 * da variável em falha de partida, com mensagem nomeando qual falta — mesmo
 * desenho do {@link br.edu.nutriclinica.config.seed.AdminProperties}.
 *
 * <p>`CORS_ALLOWED_ORIGINS` é uma lista separada por vírgula; a conversão para
 * List&lt;String&gt; é feita pelo próprio Spring.
 */
@ConfigurationProperties(prefix = "app")
@Validated
public record AppProperties(@Valid @NotNull Jwt jwt, @Valid @NotNull Cors cors) {

    /** Rejeita o placeholder que sobreviveu por a variável de ambiente não existir. */
    private static final String NAO_E_PLACEHOLDER = "^(?!\\$\\{).*$";

    public record Jwt(

            @Pattern(regexp = NAO_E_PLACEHOLDER,
                    message = "Defina JWT_SECRET: sem ela a API assinaria tokens com um segredo versionado.")
            @Size(min = 32,
                    message = "JWT_SECRET deve ter no mínimo 32 bytes (exigência do HS256).")
            String secret,

            long accessTokenMinutos,
            long refreshTokenDias) {
    }

    public record Cors(

            @NotEmpty(message = "Defina CORS_ALLOWED_ORIGINS: sem ela nenhum frontend alcança a API.")
            List<@Pattern(regexp = NAO_E_PLACEHOLDER,
                    message = "CORS_ALLOWED_ORIGINS não foi definida no ambiente.") String> allowedOrigins) {
    }
}
