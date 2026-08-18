package br.edu.nutriclinica.controller;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.Paciente;
import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.domain.enums.Perfil;
import br.edu.nutriclinica.domain.enums.Sexo;
import br.edu.nutriclinica.domain.enums.StatusAtendimento;
import br.edu.nutriclinica.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /api/pacientes: escopo de LGPD, termo de consentimento e série de evolução.
 *
 * <p>O banco é compartilhado entre as classes de teste, então as asserções são
 * sobre presença e ausência de registros específicos, nunca sobre contagem total.
 */
class PacienteControllerIT extends AbstractIntegrationTest {

    // ------------------------------------------------------------------
    // Cadastro
    // ------------------------------------------------------------------

    @Test
    @DisplayName("cadastra paciente, calcula a idade e nasce sem termo")
    void cadastra() throws Exception {
        mockMvc.perform(post("/api/pacientes")
                        .header("Authorization", "Bearer " + token(ESTAGIARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Joana Ribeiro da Silva",
                                  "dataNascimento": "2000-01-01",
                                  "sexo": "FEMININO",
                                  "racaCor": "PARDA",
                                  "telefone": "51999990000"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Joana Ribeiro da Silva"))
                .andExpect(jsonPath("$.racaCor").value("PARDA"))
                // Idade é calculada pelo servidor, não veio no corpo.
                .andExpect(jsonPath("$.idade").isNumber())
                .andExpect(jsonPath("$.possuiTermo").value(false));
    }

    @Test
    @DisplayName("nome curto demais devolve 422 apontando o campo")
    void validaNome() throws Exception {
        mockMvc.perform(post("/api/pacientes")
                        .header("Authorization", "Bearer " + token(ESTAGIARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Jo\",\"dataNascimento\":\"2000-01-01\",\"sexo\":\"FEMININO\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos[0].campo").value("nome"));
    }

    // ------------------------------------------------------------------
    // Escopo de visibilidade
    // ------------------------------------------------------------------

    @Test
    @DisplayName("estagiário não enxerga paciente cadastrado por outro: 404")
    void pacienteDeOutroEstagiario() throws Exception {
        Usuario outro = novoUsuario("Estagiário de Outra Turma", Perfil.ESTAGIARIO);
        Paciente paciente = novoPaciente(outro);

        mockMvc.perform(get("/api/pacientes/{id}", paciente.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("supervisor enxerga paciente cadastrado pelo estagiário que orienta")
    void supervisorEnxergaPacienteDoOrientado() throws Exception {
        Paciente paciente = novoPaciente(usuarioPorEmail(ESTAGIARIO));

        mockMvc.perform(get("/api/pacientes/{id}", paciente.getId())
                        .header("Authorization", "Bearer " + token(SUPERVISOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paciente.getId()));
    }

    @Test
    @DisplayName("supervisor não enxerga paciente de estagiário que não orienta: 404")
    void supervisorNaoEnxergaEstranho() throws Exception {
        Usuario estranho = novoUsuario("Estagiário Sem Vínculo", Perfil.ESTAGIARIO);
        Paciente paciente = novoPaciente(estranho);

        mockMvc.perform(get("/api/pacientes/{id}", paciente.getId())
                        .header("Authorization", "Bearer " + token(SUPERVISOR)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("admin enxerga qualquer paciente")
    void adminEnxergaTudo() throws Exception {
        Usuario estranho = novoUsuario("Estagiário Distante", Perfil.ESTAGIARIO);
        Paciente paciente = novoPaciente(estranho);

        mockMvc.perform(get("/api/pacientes/{id}", paciente.getId())
                        .header("Authorization", "Bearer " + token(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paciente.getId()));
    }

    @Test
    @DisplayName("a listagem filtra pelo escopo e responde no formato Pagina")
    void listagemNoFormatoPagina() throws Exception {
        Usuario estagiario = usuarioPorEmail(ESTAGIARIO);
        Paciente meu = novoPaciente("Aurora Vasconcelos Meu", Sexo.FEMININO, LocalDate.of(1995, 5, 5), estagiario);

        Usuario outro = novoUsuario("Estagiário Alheio", Perfil.ESTAGIARIO);
        Paciente alheio = novoPaciente("Aurora Vasconcelos Alheia", Sexo.FEMININO, LocalDate.of(1995, 5, 5), outro);

        mockMvc.perform(get("/api/pacientes")
                        .param("busca", "Aurora Vasconcelos")
                        .header("Authorization", "Bearer " + token(ESTAGIARIO)))
                .andExpect(status().isOk())
                // Formato Pagina do contrato, não o PageImpl cru do Spring.
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.pageable").doesNotExist())
                .andExpect(jsonPath("$.content[0].id").value(meu.getId()))
                .andExpect(jsonPath("$.content[*].id").value(not(hasItem(alheio.getId().intValue()))));
    }

    @Test
    @DisplayName("paciente entra no escopo do estagiário que o atendeu, mesmo sem tê-lo cadastrado")
    void pacienteEntraPeloAtendimento() throws Exception {
        Usuario dono = novoUsuario("Estagiário que Cadastrou", Perfil.ESTAGIARIO);
        Usuario estagiario = usuarioPorEmail(ESTAGIARIO);
        Paciente paciente = novoPaciente(dono);

        // Antes do atendimento, fora do escopo.
        mockMvc.perform(get("/api/pacientes/{id}", paciente.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO)))
                .andExpect(status().isNotFound());

        novoAtendimento(paciente, estagiario, usuarioPorEmail(SUPERVISOR),
                LocalDate.of(2025, 4, 1), StatusAtendimento.RASCUNHO);

        mockMvc.perform(get("/api/pacientes/{id}", paciente.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO)))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------
    // Termo de consentimento
    // ------------------------------------------------------------------

    @Test
    @DisplayName("PUT registra o termo, GET devolve, e possuiTermo passa a ser true")
    void registraTermo() throws Exception {
        Paciente paciente = novoPaciente(usuarioPorEmail(ESTAGIARIO));

        mockMvc.perform(get("/api/pacientes/{id}/termo", paciente.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO)))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/pacientes/{id}/termo", paciente.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "aceiteLgpd": true,
                                  "autorizaUsoPesquisa": true,
                                  "dataAceite": "2025-02-10",
                                  "observacoes": "Termo assinado presencialmente."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aceiteLgpd").value(true))
                .andExpect(jsonPath("$.autorizaUsoPesquisa").value(true))
                .andExpect(jsonPath("$.dataAceite").value("2025-02-10"))
                // registradoPor é o usuário autenticado, não veio no corpo.
                .andExpect(jsonPath("$.registradoPor").value("Marina Rocha"));

        mockMvc.perform(get("/api/pacientes/{id}", paciente.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.possuiTermo").value(true));
    }

    @Test
    @DisplayName("termo com aceiteLgpd false não conta como termo: possuiTermo continua false")
    void termoSemAceiteNaoConta() throws Exception {
        Paciente paciente = novoPaciente(usuarioPorEmail(ESTAGIARIO));

        mockMvc.perform(put("/api/pacientes/{id}/termo", paciente.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aceiteLgpd\":false,\"dataAceite\":\"2025-02-10\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aceiteLgpd").value(false));

        // O termo existe, mas recusado. possuiTermo é derivado do aceite, não da existência.
        mockMvc.perform(get("/api/pacientes/{id}", paciente.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.possuiTermo").value(false));
    }

    @Test
    @DisplayName("PUT do termo é upsert: o segundo envio atualiza o mesmo registro")
    void termoEhUpsert() throws Exception {
        Paciente paciente = novoPaciente(usuarioPorEmail(ESTAGIARIO));
        String token = token(ESTAGIARIO);

        mockMvc.perform(put("/api/pacientes/{id}/termo", paciente.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aceiteLgpd\":false,\"dataAceite\":\"2025-01-05\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/pacientes/{id}/termo", paciente.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aceiteLgpd\":true,\"dataAceite\":\"2025-03-05\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aceiteLgpd").value(true))
                .andExpect(jsonPath("$.dataAceite").value("2025-03-05"));
    }

    @Test
    @DisplayName("termo sem o campo de aceite devolve 422, não grava recusa por omissão")
    void termoExigeAceiteExplicito() throws Exception {
        Paciente paciente = novoPaciente(usuarioPorEmail(ESTAGIARIO));

        mockMvc.perform(put("/api/pacientes/{id}/termo", paciente.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataAceite\":\"2025-02-10\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.campos[0].campo").value("aceiteLgpd"));
    }

    @Test
    @DisplayName("não se registra termo para paciente fora do escopo: 404")
    void termoDePacienteAlheio() throws Exception {
        Usuario outro = novoUsuario("Estagiário de Outro Grupo", Perfil.ESTAGIARIO);
        Paciente paciente = novoPaciente(outro);

        mockMvc.perform(put("/api/pacientes/{id}/termo", paciente.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aceiteLgpd\":true,\"dataAceite\":\"2025-02-10\"}"))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // Evolução
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a evolução usa só atendimentos APROVADOS e sai ordenada por data de consulta")
    void evolucaoApenasAprovados() throws Exception {
        Usuario estagiario = usuarioPorEmail(ESTAGIARIO);
        Paciente paciente = novoPaciente("Paciente em Acompanhamento", Sexo.FEMININO,
                LocalDate.of(1990, 1, 1), estagiario);
        String token = token(ESTAGIARIO);

        // Criados fora de ordem cronológica para provar que quem ordena é a query.
        aferirEAprovar(paciente, LocalDate.of(2025, 6, 1), "82.00", token);
        aferirEAprovar(paciente, LocalDate.of(2025, 1, 15), "90.00", token);

        // Rascunho: o peso ainda pode estar errado, não entra na curva.
        Atendimento rascunho = novoAtendimento(paciente, estagiario, usuarioPorEmail(SUPERVISOR),
                LocalDate.of(2025, 9, 1), StatusAtendimento.RASCUNHO);
        aferir(rascunho, "70.00", token);

        // 1,75 m: 90 kg => IMC 29,39; 82 kg => 26,78.
        mockMvc.perform(get("/api/pacientes/{id}/evolucao", paciente.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].dataConsulta").value("2025-01-15"))
                .andExpect(jsonPath("$[0].pesoKg").value(90.00))
                .andExpect(jsonPath("$[0].imc").value(29.39))
                .andExpect(jsonPath("$[1].dataConsulta").value("2025-06-01"))
                .andExpect(jsonPath("$[1].pesoKg").value(82.00))
                .andExpect(jsonPath("$[1].imc").value(26.78));
    }

    @Test
    @DisplayName("evolução de paciente fora do escopo devolve 404")
    void evolucaoDePacienteAlheio() throws Exception {
        Usuario outro = novoUsuario("Estagiário Terceiro", Perfil.ESTAGIARIO);
        Paciente paciente = novoPaciente(outro);

        mockMvc.perform(get("/api/pacientes/{id}/evolucao", paciente.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO)))
                .andExpect(status().isNotFound());
    }

    /**
     * Atendimento com antropometria aferida pelo endpoint — o IMC da série é o
     * que o servidor calculou — e depois aprovado, que é o estado que a evolução
     * considera.
     */
    private void aferirEAprovar(Paciente paciente, LocalDate dataConsulta, String pesoKg, String token)
            throws Exception {
        Atendimento atendimento = novoAtendimento(paciente, usuarioPorEmail(ESTAGIARIO),
                usuarioPorEmail(SUPERVISOR), dataConsulta, StatusAtendimento.RASCUNHO);
        aferir(atendimento, pesoKg, token);

        atendimento.setStatus(StatusAtendimento.APROVADO);
        atendimentoRepository.save(atendimento);
    }

    private void aferir(Atendimento atendimento, String pesoKg, String token) throws Exception {
        mockMvc.perform(patch("/api/atendimentos/{id}/antropometria", atendimento.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pesoKg\":" + pesoKg + ",\"alturaCm\":175.0}"))
                .andExpect(status().isOk());
    }
}
