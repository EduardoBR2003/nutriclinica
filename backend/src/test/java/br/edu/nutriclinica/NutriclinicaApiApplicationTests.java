package br.edu.nutriclinica;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test da Etapa 0: garante que o contexto sobe (web + security + springdoc).
 * A camada de persistência é excluída aqui — os testes com banco real via
 * Testcontainers entram na Etapa 3, junto com as migrations e as entidades.
 */
@SpringBootTest
@ActiveProfiles("test")
class NutriclinicaApiApplicationTests {

    @Test
    void contextLoads() {
    }
}
