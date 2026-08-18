package br.edu.nutriclinica;

import br.edu.nutriclinica.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Smoke test: garante que o contexto completo sobe — web, security, JPA, Flyway
 * e springdoc — contra um PostgreSQL real. Desde o bloco de autenticação a camada
 * de segurança depende dos repositories, então não faz mais sentido subir sem banco.
 */
class NutriclinicaApiApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}
