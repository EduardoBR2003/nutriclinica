package br.edu.nutriclinica.controller;

import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.domain.enums.Perfil;
import br.edu.nutriclinica.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /api/usuarios, /api/usuarios/{id}/vinculos e o auto-cadastro público.
 *
 * <p>O banco é compartilhado entre as classes de teste, então as asserções são
 * sobre presença e ausência de registros específicos, nunca sobre contagem total.
 */
class UsuarioControllerIT extends AbstractIntegrationTest {

    // ------------------------------------------------------------------
    // Cadastro pelo ADMIN
    // ------------------------------------------------------------------

    @Test
    @DisplayName("ADMIN cadastra usuário, que já nasce ativo e consegue entrar")
    void adminCadastra() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Carla Nogueira",
                                  "email": "Carla.Nogueira@nutriclinica.edu.br",
                                  "senha": "senha123",
                                  "perfil": "SUPERVISOR"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.perfil").value("SUPERVISOR"))
                .andExpect(jsonPath("$.ativo").value(true))
                // O e-mail é normalizado: senão, o mesmo endereço viraria dois logins.
                .andExpect(jsonPath("$.email").value("carla.nogueira@nutriclinica.edu.br"));

        // Nasce ativo e com a senha utilizável: o login é a prova das duas coisas.
        assertThat(token("carla.nogueira@nutriclinica.edu.br")).isNotBlank();
    }

    @Test
    @DisplayName("cadastro sem senha devolve 422 no campo senha")
    void cadastroExigeSenha() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Sem Senha", "email": "sem-senha@nutriclinica.edu.br",
                                 "perfil": "ESTAGIARIO"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos[0].campo").value("senha"));
    }

    @Test
    @DisplayName("e-mail repetido, mesmo com outra caixa, devolve 422 no campo email")
    void emailRepetido() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Sósia do Estagiário",
                                 "email": "ESTAGIARIO@nutriclinica.edu.br",
                                 "senha": "senha123", "perfil": "ESTAGIARIO"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.campos[0].campo").value("email"));
    }

    @Test
    @DisplayName("PUT sem senha mantém a atual; com senha nova, troca")
    void atualizaUsuario() throws Exception {
        Usuario usuario = novoUsuario("Nome Antigo", Perfil.ESTAGIARIO);
        String email = usuario.getEmail();
        String tokenAdmin = token(ADMIN);

        mockMvc.perform(put("/api/usuarios/{id}", usuario.getId())
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Nome Corrigido", "email": "%s",
                                 "perfil": "ESTAGIARIO", "ativo": true}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nome Corrigido"));

        // Sem senha no corpo, a antiga continua valendo.
        assertThat(token(email)).isNotBlank();

        mockMvc.perform(put("/api/usuarios/{id}", usuario.getId())
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Nome Corrigido", "email": "%s", "senha": "outrasenha1",
                                 "perfil": "ESTAGIARIO", "ativo": true}
                                """.formatted(email)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, SENHA)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("usuário desativado pelo ADMIN não entra mais")
    void desativaUsuario() throws Exception {
        Usuario usuario = novoUsuario("Estagiário de Saída", Perfil.ESTAGIARIO);

        mockMvc.perform(put("/api/usuarios/{id}", usuario.getId())
                        .header("Authorization", "Bearer " + token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Estagiário de Saída", "email": "%s",
                                 "perfil": "ESTAGIARIO", "ativo": false}
                                """.formatted(usuario.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"%s\"}"
                                .formatted(usuario.getEmail(), SENHA)))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // Listagem
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a lista filtra por perfil e sai no schema Pagina")
    void listaPorPerfil() throws Exception {
        Usuario supervisor = novoUsuario("Supervisor Listado", Perfil.SUPERVISOR);
        Usuario estagiario = novoUsuario("Estagiário Listado", Perfil.ESTAGIARIO);

        mockMvc.perform(get("/api/usuarios")
                        .param("perfil", "SUPERVISOR")
                        .param("size", "100")
                        .header("Authorization", "Bearer " + token(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.content[*].nome", hasItem(supervisor.getNome())))
                .andExpect(jsonPath("$.content[*].nome", not(hasItem(estagiario.getNome()))));
    }

    @Test
    @DisplayName("/api/usuarios é só do ADMIN: estagiário e supervisor levam 403")
    void listaExclusivaDoAdmin() throws Exception {
        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer " + token(ESTAGIARIO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));

        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer " + token(SUPERVISOR)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // Vínculos
    // ------------------------------------------------------------------

    @Test
    @DisplayName("PUT define os vínculos, GET devolve o conjunto atual")
    void defineVinculos() throws Exception {
        Usuario supervisor = novoUsuario("Supervisora de Vínculos", Perfil.SUPERVISOR);
        Usuario ana = novoUsuario("Ana Vinculada", Perfil.ESTAGIARIO);
        Usuario bruno = novoUsuario("Bruno Vinculado", Perfil.ESTAGIARIO);
        String tokenAdmin = token(ADMIN);

        mockMvc.perform(put("/api/usuarios/{id}/vinculos", supervisor.getId())
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estagiarioIds\":[%d,%d]}".formatted(ana.getId(), bruno.getId())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/usuarios/{id}/vinculos", supervisor.getId())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].nome", hasItem("Ana Vinculada")));

        // Substituição, não acréscimo: quem saiu da lista sai do vínculo.
        mockMvc.perform(put("/api/usuarios/{id}/vinculos", supervisor.getId())
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estagiarioIds\":[%d]}".formatted(bruno.getId())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/usuarios/{id}/vinculos", supervisor.getId())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Bruno Vinculado"));

        // Devolver quem havia saído reaproveita a linha: o UNIQUE (supervisor,
        // estagiário) não admite uma segunda, e um INSERT cego quebraria aqui.
        mockMvc.perform(put("/api/usuarios/{id}/vinculos", supervisor.getId())
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estagiarioIds\":[%d,%d]}".formatted(ana.getId(), bruno.getId())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/usuarios/{id}/vinculos", supervisor.getId())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("vincular quem não é estagiário devolve 422; vincular a quem não é supervisor também")
    void vinculosValidamPerfis() throws Exception {
        Usuario supervisor = novoUsuario("Supervisor Rigoroso", Perfil.SUPERVISOR);
        Usuario outroSupervisor = novoUsuario("Supervisor Impróprio", Perfil.SUPERVISOR);
        Usuario estagiario = novoUsuario("Estagiário Comum", Perfil.ESTAGIARIO);
        String tokenAdmin = token(ADMIN);

        mockMvc.perform(put("/api/usuarios/{id}/vinculos", supervisor.getId())
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estagiarioIds\":[%d]}".formatted(outroSupervisor.getId())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.campos[0].campo").value("estagiarioIds"));

        mockMvc.perform(put("/api/usuarios/{id}/vinculos", estagiario.getId())
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estagiarioIds\":[]}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("trocar o perfil de um supervisor desfaz os vínculos dele")
    void trocaDePerfilDesfazVinculos() throws Exception {
        Usuario supervisor = novoUsuario("Supervisor Transferido", Perfil.SUPERVISOR);
        Usuario estagiario = novoUsuario("Orientado Órfão", Perfil.ESTAGIARIO);
        vincular(supervisor, estagiario);

        mockMvc.perform(put("/api/usuarios/{id}", supervisor.getId())
                        .header("Authorization", "Bearer " + token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Supervisor Transferido", "email": "%s",
                                 "perfil": "ADMIN", "ativo": true}
                                """.formatted(supervisor.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("ADMIN"));

        assertThat(vinculoSupervisaoRepository
                .existsBySupervisorIdAndEstagiarioIdAndAtivoTrue(supervisor.getId(), estagiario.getId()))
                .isFalse();
    }

    // ------------------------------------------------------------------
    // Supervisores disponíveis ao estagiário
    // ------------------------------------------------------------------

    @Test
    @DisplayName("o estagiário vê só os supervisores que o orientam, e ativos")
    void supervisoresDisponiveis() throws Exception {
        Usuario estranho = novoUsuario("Supervisor de Outro Grupo", Perfil.SUPERVISOR);

        mockMvc.perform(get("/api/usuarios/supervisores")
                        .header("Authorization", "Bearer " + token(ESTAGIARIO)))
                .andExpect(status().isOk())
                // O vínculo do seed: Profa. Helena Duarte orienta Marina Rocha.
                .andExpect(jsonPath("$[*].nome", hasItem("Profa. Helena Duarte")))
                .andExpect(jsonPath("$[*].nome", not(hasItem(estranho.getNome()))));
    }

    @Test
    @DisplayName("supervisor desativado sai da lista de escolha do estagiário")
    void supervisorInativoNaoAparece() throws Exception {
        Usuario supervisor = novoUsuario("Supervisor Afastado", Perfil.SUPERVISOR);
        Usuario estagiario = novoUsuario("Estagiário Sozinho", Perfil.ESTAGIARIO);
        vincular(supervisor, estagiario);
        String token = token(estagiario.getEmail());

        mockMvc.perform(get("/api/usuarios/supervisores")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nome", hasItem("Supervisor Afastado")));

        mockMvc.perform(put("/api/usuarios/{id}", supervisor.getId())
                        .header("Authorization", "Bearer " + token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Supervisor Afastado", "email": "%s",
                                 "perfil": "SUPERVISOR", "ativo": false}
                                """.formatted(supervisor.getEmail())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/usuarios/supervisores")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ------------------------------------------------------------------
    // Auto-cadastro
    // ------------------------------------------------------------------

    @Test
    @DisplayName("auto-cadastro é público, nasce inativo e não entra até o ADMIN ativar")
    void autoCadastro() throws Exception {
        String corpo = """
                {"nome": "Pedro Candidato", "email": "pedro.candidato@nutriclinica.edu.br",
                 "senha": "senha123", "perfil": "ESTAGIARIO"}
                """;

        mockMvc.perform(post("/api/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").value("ESTAGIARIO"))
                .andExpect(jsonPath("$.ativo").value(false));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"pedro.candidato@nutriclinica.edu.br\",\"senha\":\"senha123\"}"))
                .andExpect(status().isUnauthorized());

        Usuario criado = usuarioPorEmail("pedro.candidato@nutriclinica.edu.br");
        mockMvc.perform(put("/api/usuarios/{id}", criado.getId())
                        .header("Authorization", "Bearer " + token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Pedro Candidato", "email": "pedro.candidato@nutriclinica.edu.br",
                                 "perfil": "ESTAGIARIO", "ativo": true}
                                """))
                .andExpect(status().isOk());

        assertThat(token("pedro.candidato@nutriclinica.edu.br")).isNotBlank();
    }

    @Test
    @DisplayName("ninguém se auto-cadastra como ADMIN")
    void autoCadastroNaoCriaAdmin() throws Exception {
        mockMvc.perform(post("/api/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Admin Espontâneo", "email": "espontaneo@nutriclinica.edu.br",
                                 "senha": "senha123", "perfil": "ADMIN"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.campos[0].campo").value("perfil"));
    }

    @Test
    @DisplayName("auto-cadastro com senha curta devolve 422")
    void autoCadastroExigeSenhaForte() throws Exception {
        mockMvc.perform(post("/api/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Senha Curta", "email": "curta@nutriclinica.edu.br",
                                 "senha": "1234", "perfil": "ESTAGIARIO"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.campos[0].campo").value("senha"));
    }
}
