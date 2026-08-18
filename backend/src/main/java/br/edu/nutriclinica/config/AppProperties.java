package br.edu.nutriclinica.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuração da aplicação, prefixo `app`. O segredo do JWT e as origens de CORS
 * vêm das variáveis de ambiente JWT_SECRET e CORS_ALLOWED_ORIGINS — fora do perfil
 * dev não há valor padrão, então a aplicação falha ao subir se faltarem.
 *
 * `CORS_ALLOWED_ORIGINS` é uma lista separada por vírgula; a conversão para
 * List&lt;String&gt; é feita pelo próprio Spring.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors) {

    public record Jwt(String secret, long accessTokenMinutos, long refreshTokenDias) {
    }

    public record Cors(List<String> allowedOrigins) {
    }
}
