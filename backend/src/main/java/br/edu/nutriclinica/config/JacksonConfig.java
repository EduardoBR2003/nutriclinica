package br.edu.nutriclinica.config;

import io.swagger.v3.core.converter.ModelConverter;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensina o Jackson a desserializar {@code JsonNullable}, o tipo que sustenta a
 * semântica do PATCH por seção: campo ausente no corpo mantém o valor atual,
 * campo presente com {@code null} limpa o valor.
 *
 * <p>O módulo é declarado como bean e não descoberto por ServiceLoader porque o
 * Spring Boot só instala automaticamente os módulos que existem no contexto.
 *
 * <p>O {@code JsonNullableValueExtractor} que acompanha a mesma dependência é
 * registrado sozinho, via {@code META-INF/services}, e é o que faz uma constraint
 * escrita no elemento do container — {@code JsonNullable<@DecimalMin("1")
 * BigDecimal>} — continuar valendo.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public JsonNullableModule jsonNullableModule() {
        return new JsonNullableModule();
    }

    /** Mantém o Swagger falando dos campos como tipos simples, igual ao contrato. */
    @Bean
    public ModelConverter jsonNullableModelConverter() {
        return new JsonNullableModelConverter();
    }
}
