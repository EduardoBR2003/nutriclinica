package br.edu.nutriclinica.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_BEARER = "bearerAuth";

    @Bean
    public OpenAPI nutriclinicaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NutriClinica API")
                        .description("API do sistema de prontuário da clínica escola de nutrição")
                        .version("v0.0.1")
                        .license(new License().name("Uso acadêmico")))
                // Igual ao docs/api.yaml: bearer JWT exigido por padrão, exceto onde
                // o endpoint declara @SecurityRequirements vazio (login e refresh).
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BEARER))
                .components(new Components().addSecuritySchemes(ESQUEMA_BEARER,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
