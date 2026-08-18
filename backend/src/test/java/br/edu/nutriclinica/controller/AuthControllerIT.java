package br.edu.nutriclinica.controller;

import br.edu.nutriclinica.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Fluxo de autenticação sobre os usuários do V2__seed.sql. */
class AuthControllerIT extends AbstractIntegrationTest {

    private static final String ESTAGIARIO = "estagiario@nutriclinica.edu.br";
    private static final String SENHA = "senha123";

    @Test
    @DisplayName("login com credenciais válidas devolve 200 com o par de tokens")
    void loginValido() throws Exception {
        String corpo = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(ESTAGIARIO, SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiraEm").value(900))
                .andExpect(jsonPath("$.usuario.email").value(ESTAGIARIO))
                .andExpect(jsonPath("$.usuario.perfil").value("ESTAGIARIO"))
                .andReturn().getResponse().getContentAsString();

        JsonNode resposta = objectMapper.readTree(corpo);
        assertThat(resposta.get("accessToken").asText()).isNotBlank();
        assertThat(resposta.get("refreshToken").asText()).isNotBlank();
        // A senha, mesmo em hash, nunca pode aparecer na resposta.
        assertThat(corpo).doesNotContain("senhaHash");
    }

    @Test
    @DisplayName("login com senha errada devolve 401 no formato Erro")
    void loginInvalido() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(ESTAGIARIO, "senha-errada")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTORIZADO"))
                .andExpect(jsonPath("$.mensagem").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("rota protegida sem token devolve 401 no formato Erro")
    void semToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTORIZADO"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("/me com o access token do login devolve o usuário autenticado")
    void meComToken() throws Exception {
        JsonNode tokens = autenticar();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + tokens.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(ESTAGIARIO))
                .andExpect(jsonPath("$.perfil").value("ESTAGIARIO"));
    }

    @Test
    @DisplayName("refresh rotaciona o token; reapresentar o revogado devolve 401")
    void refreshComTokenRevogado() throws Exception {
        String refreshOriginal = autenticar().get("refreshToken").asText();

        // Primeiro uso: válido, revoga o antigo e emite um par novo.
        String corpoRotacionado = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshOriginal + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String refreshNovo = objectMapper.readTree(corpoRotacionado).get("refreshToken").asText();
        assertThat(refreshNovo).isNotEqualTo(refreshOriginal);

        // Segundo uso do mesmo token: agora revogado.
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshOriginal + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTORIZADO"));
    }

    @Test
    @DisplayName("login sem corpo válido devolve 422 com o array campos preenchido")
    void loginSemCampos() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos").isArray())
                .andExpect(jsonPath("$.campos[0].campo").isNotEmpty())
                .andExpect(jsonPath("$.campos[0].mensagem").isNotEmpty());
    }

    private JsonNode autenticar() throws Exception {
        String corpo = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(ESTAGIARIO, SENHA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo);
    }

    private String json(String email, String senha) {
        return "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}";
    }
}
