package br.edu.nutriclinica.controller;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.Paciente;
import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.domain.enums.Perfil;
import br.edu.nutriclinica.domain.enums.StatusAtendimento;
import br.edu.nutriclinica.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.RequestBuilder;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /api/atendimentos: escopo de LGPD, pré-condição do termo, validação do
 * supervisor e numeração de prontuário.
 */
class AtendimentoControllerIT extends AbstractIntegrationTest {

    // ------------------------------------------------------------------
    // Abertura do atendimento
    // ------------------------------------------------------------------

    @Test
    @DisplayName("abre atendimento em RASCUNHO, editável pelo estagiário e sem seções preenchidas")
    void abreAtendimento() throws Exception {
        Paciente paciente = pacienteComTermo();

        mockMvc.perform(post("/api/atendimentos")
                        .header("Authorization", "Bearer " + token(ESTAGIARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(paciente, usuarioPorEmail(SUPERVISOR), "2025-05-20")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RASCUNHO"))
                .andExpect(jsonPath("$.editavel").value(true))
                .andExpect(jsonPath("$.secoesPreenchidas").isEmpty())
                .andExpect(jsonPath("$.numeroProntuario").value(matchesPattern("2025-\\d{6}")))
                .andExpect(jsonPath("$.paciente.id").value(paciente.getId()))
                .andExpect(jsonPath("$.estagiario.email").value(ESTAGIARIO))
                .andExpect(jsonPath("$.supervisor.email").value(SUPERVISOR))
                .andExpect(jsonPath("$.submetidoEm").doesNotExist());
    }

    @Test
    @DisplayName("paciente sem termo LGPD: 409 SEM_TERMO_CONSENTIMENTO")
    void semTermoDeConsentimento() throws Exception {
        Paciente paciente = novoPaciente(usuarioPorEmail(ESTAGIARIO));

        mockMvc.perform(post("/api/atendimentos")
                        .header("Authorization", "Bearer " + token(ESTAGIARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(paciente, usuarioPorEmail(SUPERVISOR), "2025-05-20")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SEM_TERMO_CONSENTIMENTO"));
    }

    @Test
    @DisplayName("termo com aceiteLgpd false não libera a abertura: 409")
    void termoRecusadoNaoLibera() throws Exception {
        Usuario estagiario = usuarioPorEmail(ESTAGIARIO);
        Paciente paciente = novoPaciente(estagiario);
        registrarTermo(paciente, estagiario, false);

        mockMvc.perform(post("/api/atendimentos")
                        .header("Authorization", "Bearer " + token(ESTAGIARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(paciente, usuarioPorEmail(SUPERVISOR), "2025-05-20")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SEM_TERMO_CONSENTIMENTO"));
    }

    @Test
    @DisplayName("supervisor sem vínculo com o estagiário: 422 apontando supervisorId")
    void supervisorSemVinculo() throws Exception {
        Paciente paciente = pacienteComTermo();
        Usuario outroSupervisor = novoUsuario("Prof. Sem Vínculo", Perfil.SUPERVISOR);

        mockMvc.perform(post("/api/atendimentos")
                        .header("Authorization", "Bearer " + token(ESTAGIARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(paciente, outroSupervisor, "2025-05-20")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos[0].campo").value("supervisorId"));
    }

    @Test
    @DisplayName("supervisorId apontando para um estagiário: 422")
    void supervisorComPerfilErrado() throws Exception {
        Paciente paciente = pacienteComTermo();
        Usuario naoSupervisor = novoUsuario("Colega Estagiário", Perfil.ESTAGIARIO);

        mockMvc.perform(post("/api/atendimentos")
                        .header("Authorization", "Bearer " + token(ESTAGIARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(paciente, naoSupervisor, "2025-05-20")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.campos[0].campo").value("supervisorId"));
    }

    @Test
    @DisplayName("paciente fora do escopo devolve 404, antes de revelar se tem termo")
    void pacienteForaDoEscopo() throws Exception {
        Usuario outro = novoUsuario("Estagiário de Outro Grupo", Perfil.ESTAGIARIO);
        Paciente paciente = novoPaciente(outro);
        registrarTermo(paciente, outro, true);

        mockMvc.perform(post("/api/atendimentos")
                        .header("Authorization", "Bearer " + token(ESTAGIARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(paciente, usuarioPorEmail(SUPERVISOR), "2025-05-20")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("supervisor não abre atendimento: 403")
    void supervisorNaoAbreAtendimento() throws Exception {
        Paciente paciente = pacienteComTermo();

        mockMvc.perform(post("/api/atendimentos")
                        .header("Authorization", "Bearer " + token(SUPERVISOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(paciente, usuarioPorEmail(SUPERVISOR), "2025-05-20")))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // Numeração de prontuário
    // ------------------------------------------------------------------

    @Test
    @DisplayName("numeração é sequencial dentro do ano e reinicia no ano seguinte")
    void numeracaoSequencialPorAno() throws Exception {
        // Anos exclusivos deste teste: o contador é global e o banco é
        // compartilhado entre as classes, então 2025 já foi usado por outros.
        Paciente paciente = pacienteComTermo();
        String token = token(ESTAGIARIO);
        Usuario supervisor = usuarioPorEmail(SUPERVISOR);

        mockMvc.perform(criar(token, paciente, supervisor, "2019-03-01"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroProntuario").value("2019-000001"));

        mockMvc.perform(criar(token, paciente, supervisor, "2019-11-20"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroProntuario").value("2019-000002"));

        // Ano novo, contagem do zero — e o AAAA sai da data da consulta.
        mockMvc.perform(criar(token, paciente, supervisor, "2020-01-02"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroProntuario").value("2020-000001"));

        // A série de 2019 continua de onde parou.
        mockMvc.perform(criar(token, paciente, supervisor, "2019-12-31"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroProntuario").value("2019-000003"));
    }

    // ------------------------------------------------------------------
    // Escopo de visibilidade
    // ------------------------------------------------------------------

    @Test
    @DisplayName("estagiário A não enxerga atendimento do estagiário B: 404")
    void atendimentoDeOutroEstagiario() throws Exception {
        Atendimento alheio = atendimentoDeOutroEstagiarioNoBanco();

        // 404 e não 403: um 403 confirmaria que esse prontuário existe.
        mockMvc.perform(get("/api/atendimentos/{id}", alheio.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("a listagem do estagiário não traz o atendimento de outro")
    void listagemNaoVazaAtendimentoAlheio() throws Exception {
        Atendimento alheio = atendimentoDeOutroEstagiarioNoBanco();

        mockMvc.perform(get("/api/atendimentos")
                        .param("size", "100")
                        .header("Authorization", "Bearer " + token(ESTAGIARIO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(not(hasItem(alheio.getId().intValue()))));
    }

    @Test
    @DisplayName("supervisor enxerga o atendimento do seu orientado")
    void supervisorEnxergaOrientado() throws Exception {
        Usuario estagiario = usuarioPorEmail(ESTAGIARIO);
        // Supervisor designado é outro: o acesso aqui vem do vínculo de
        // supervisão com o estagiário, não do supervisor_id do atendimento.
        Usuario outroSupervisor = novoUsuario("Prof. Substituto", Perfil.SUPERVISOR);
        Paciente paciente = novoPaciente(estagiario);
        Atendimento atendimento = novoAtendimento(paciente, estagiario, outroSupervisor,
                LocalDate.of(2025, 7, 7), StatusAtendimento.EM_REVISAO);

        mockMvc.perform(get("/api/atendimentos/{id}", atendimento.getId())
                        .header("Authorization", "Bearer " + token(SUPERVISOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(atendimento.getId()))
                // Somente leitura para quem não é o estagiário dono.
                .andExpect(jsonPath("$.editavel").value(false));
    }

    @Test
    @DisplayName("supervisor não enxerga atendimento de estagiário que não orienta: 404")
    void supervisorNaoEnxergaEstranho() throws Exception {
        Atendimento alheio = atendimentoDeOutroEstagiarioNoBanco();

        mockMvc.perform(get("/api/atendimentos/{id}", alheio.getId())
                        .header("Authorization", "Bearer " + token(SUPERVISOR)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("admin enxerga qualquer atendimento, sem poder editá-lo")
    void adminEnxergaTudo() throws Exception {
        Atendimento alheio = atendimentoDeOutroEstagiarioNoBanco();

        mockMvc.perform(get("/api/atendimentos/{id}", alheio.getId())
                        .header("Authorization", "Bearer " + token(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alheio.getId()))
                .andExpect(jsonPath("$.editavel").value(false));
    }

    // ------------------------------------------------------------------
    // Campos derivados e filtros
    // ------------------------------------------------------------------

    @Test
    @DisplayName("editavel acompanha o status: false em EM_REVISAO e APROVADO")
    void editavelAcompanhaOStatus() throws Exception {
        Usuario estagiario = usuarioPorEmail(ESTAGIARIO);
        Usuario supervisor = usuarioPorEmail(SUPERVISOR);
        Paciente paciente = novoPaciente(estagiario);

        Atendimento devolvido = novoAtendimento(paciente, estagiario, supervisor,
                LocalDate.of(2025, 2, 1), StatusAtendimento.DEVOLVIDO_PARA_CORRECAO);
        Atendimento emRevisao = novoAtendimento(paciente, estagiario, supervisor,
                LocalDate.of(2025, 2, 2), StatusAtendimento.EM_REVISAO);
        Atendimento aprovado = novoAtendimento(paciente, estagiario, supervisor,
                LocalDate.of(2025, 2, 3), StatusAtendimento.APROVADO);

        String token = token(ESTAGIARIO);
        esperarEditavel(token, devolvido, true);
        esperarEditavel(token, emRevisao, false);
        esperarEditavel(token, aprovado, false);
    }

    @Test
    @DisplayName("secoesPreenchidas passa a listar ANTROPOMETRIA depois do PATCH da seção")
    void secoesPreenchidas() throws Exception {
        Usuario estagiario = usuarioPorEmail(ESTAGIARIO);
        Paciente paciente = novoPaciente(estagiario);
        Atendimento atendimento = novoAtendimento(paciente, estagiario, usuarioPorEmail(SUPERVISOR),
                LocalDate.of(2025, 8, 8), StatusAtendimento.RASCUNHO);
        String token = token(ESTAGIARIO);

        mockMvc.perform(get("/api/atendimentos/{id}", atendimento.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secoesPreenchidas").isEmpty())
                .andExpect(jsonPath("$.antropometria").doesNotExist());

        mockMvc.perform(patch("/api/atendimentos/{id}/antropometria", atendimento.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pesoKg\":72.00,\"alturaCm\":170.0}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/atendimentos/{id}", atendimento.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secoesPreenchidas.length()").value(1))
                .andExpect(jsonPath("$.secoesPreenchidas[0]").value("ANTROPOMETRIA"))
                // O prontuário completo traz a seção com os indicadores calculados.
                .andExpect(jsonPath("$.antropometria.pesoKg").value(72.00))
                .andExpect(jsonPath("$.antropometria.imc").value(24.91));
    }

    @Test
    @DisplayName("filtros de status e pacienteId restringem a listagem")
    void filtros() throws Exception {
        Usuario estagiario = usuarioPorEmail(ESTAGIARIO);
        Usuario supervisor = usuarioPorEmail(SUPERVISOR);
        Paciente paciente = novoPaciente(estagiario);

        Atendimento rascunho = novoAtendimento(paciente, estagiario, supervisor,
                LocalDate.of(2025, 3, 1), StatusAtendimento.RASCUNHO);
        Atendimento aprovado = novoAtendimento(paciente, estagiario, supervisor,
                LocalDate.of(2025, 3, 2), StatusAtendimento.APROVADO);

        mockMvc.perform(get("/api/atendimentos")
                        .param("pacienteId", paciente.getId().toString())
                        .param("status", "APROVADO")
                        .header("Authorization", "Bearer " + token(ESTAGIARIO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(aprovado.getId()));

        mockMvc.perform(get("/api/atendimentos")
                        .param("pacienteId", paciente.getId().toString())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].id").value(hasItem(rascunho.getId().intValue())));
    }

    @Test
    @DisplayName("sem token não passa da cadeia de filtros: 401")
    void semToken() throws Exception {
        mockMvc.perform(get("/api/atendimentos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTORIZADO"));
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private Paciente pacienteComTermo() {
        Usuario estagiario = usuarioPorEmail(ESTAGIARIO);
        Paciente paciente = novoPaciente(estagiario);
        registrarTermo(paciente, estagiario, true);
        return paciente;
    }

    /** Atendimento de um estagiário sem nenhum vínculo com o supervisor do seed. */
    private Atendimento atendimentoDeOutroEstagiarioNoBanco() {
        Usuario outroEstagiario = novoUsuario("Estagiário de Outra Turma", Perfil.ESTAGIARIO);
        Usuario outroSupervisor = novoUsuario("Prof. de Outra Turma", Perfil.SUPERVISOR);
        vincular(outroSupervisor, outroEstagiario);

        Paciente paciente = novoPaciente(outroEstagiario);
        return novoAtendimento(paciente, outroEstagiario, outroSupervisor,
                LocalDate.of(2025, 6, 6), StatusAtendimento.RASCUNHO);
    }

    private void esperarEditavel(String token, Atendimento atendimento, boolean editavel) throws Exception {
        mockMvc.perform(get("/api/atendimentos/{id}", atendimento.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.editavel").value(editavel));
    }

    private RequestBuilder criar(String token,
                                 Paciente paciente,
                                 Usuario supervisor,
                                 String dataConsulta) {
        return post("/api/atendimentos")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(paciente, supervisor, dataConsulta));
    }

    private String corpo(Paciente paciente, Usuario supervisor, String dataConsulta) {
        return """
                {"pacienteId": %d, "supervisorId": %d, "dataConsulta": "%s"}
                """.formatted(paciente.getId(), supervisor.getId(), dataConsulta);
    }
}
