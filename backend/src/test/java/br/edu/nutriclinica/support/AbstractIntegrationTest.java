package br.edu.nutriclinica.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base dos testes de integração: PostgreSQL 17 real via Testcontainers, migrations
 * do Flyway aplicadas (inclusive o seed do V2) e MockMvc passando pela cadeia de
 * filtros do Spring Security.
 *
 * O container é estático e não é fechado de propósito: o Testcontainers o reaproveita
 * entre as classes de teste e o encerra ao fim da JVM (Ryuk).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;
}
