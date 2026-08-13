package br.edu.nutriclinica.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI nutriclinicaOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("NutriClinica API")
                .description("API do sistema de prontuário da clínica escola de nutrição")
                .version("v0.0.1")
                .license(new License().name("Uso acadêmico")));
    }
}
