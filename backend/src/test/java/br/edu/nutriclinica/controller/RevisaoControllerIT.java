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

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A fila de revisão e os comentários por seção — o que o supervisor vê e
 * escreve.
 */
class RevisaoControllerIT extends AbstractIntegrationTest {

    // ------------------------------------------------------------------
    // Fila de pendentes
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a fila traz só os EM_REVISAO do supervisor, do mais antigo para o mais novo")
    void filaOrdenada() throws Exception {
        // Supervisor novo: a fila dele contém exatamente o que este teste criou,
        // o que permite assertar ordem sem depender do estado do banco.
        Usuario supervisor = novoUsuario("Profa. da Fila", Perfil.SUPERVISOR);
        Usuario estagiario = novoUsuario("Estagiário da Fila", Perfil.ESTAGIARIO);
        vincular(supervisor, estagiario);
        Paciente paciente = novoPaciente(estagiario);

        Atendimento antigo = emRevisaoDesde(paciente, estagiario, supervisor,
                LocalDate.of(2025, 3, 1), LocalDateTime.of(2025, 3, 1, 9, 0));
        Atendimento recente = emRevisaoDesde(paciente, estagiario, supervisor,
                LocalDate.of(2025, 3, 2), LocalDateTime.of(2025, 3, 5, 9, 0));
        Atendimento rascunho = novoAtendimento(paciente, estagiario, supervisor,
                LocalDate.of(2025, 3, 3), StatusAtendimento.RASCUNHO);

        mockMvc.perform(get("/api/revisoes/pendentes")
                        .header("Authorization", "Bearer " + token(supervisor.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(antigo.getId()))
                .andExpect(jsonPath("$.content[1].id").value(recente.getId()))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(rascunho.getId().intValue()))))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("orientar o estagiário não põe o prontuário na fila de quem não é o designado")
    void filaNaoVazaDeOutroSupervisor() throws Exception {
        Usuario designado = novoUsuario("Profa. Designada", Perfil.SUPERVISOR);
        Usuario espectador = novoUsuario("Profa. Espectadora", Perfil.SUPERVISOR);
        Usuario estagiario = novoUsuario("Estagiário Compartilhado", Perfil.ESTAGIARIO);
        vincular(designado, estagiario);
        vincular(espectador, estagiario);

        Atendimento atendimento = emRevisaoDesde(novoPaciente(estagiario), estagiario, designado,
                LocalDate.of(2025, 4, 1), LocalDateTime.of(2025, 4, 1, 9, 0));

        // O espectador enxerga o atendimento (orienta o estagiário), mas revisar
        // é de quem foi designado — a fila de trabalho não é a mesma coisa que o
        // escopo de leitura.
        mockMvc.perform(get("/api/revisoes/pendentes")
                        .header("Authorization", "Bearer " + token(espectador.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", not(hasItem(atendimento.getId().intValue()))));

        mockMvc.perform(get("/api/atendimentos/{id}", atendimento.getId())
                        .header("Authorization", "Bearer " + token(espectador.getEmail())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("estagiário não tem fila de revisão: 403")
    void estagiarioSemFila() throws Exception {
        mockMvc.perform(get("/api/revisoes/pendentes")
                        .header("Authorization", "Bearer " + token(ESTAGIARIO)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // Comentários
    // ------------------------------------------------------------------

    @Test
    @DisplayName("supervisor designado comenta e o estagiário lê, em ordem cronológica")
    void supervisorComenta() throws Exception {
        Atendimento atendimento = emRevisaoDoSeed();
        String tokenSupervisor = token(SUPERVISOR);

        comentar(atendimento, tokenSupervisor, "DIAGNOSTICO", "Reveja a etiologia do PES.")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.secao").value("DIAGNOSTICO"))
                .andExpect(jsonPath("$.autor.email").value(SUPERVISOR))
                .andExpect(jsonPath("$.resolvido").value(false))
                .andExpect(jsonPath("$.criadoEm").exists());

        comentar(atendimento, tokenSupervisor, "PLANO", "Detalhe melhor as estratégias.")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/atendimentos/{id}/comentarios", atendimento.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].secao").value("DIAGNOSTICO"))
                .andExpect(jsonPath("$[1].secao").value("PLANO"));
    }

    @Test
    @DisplayName("estagiário lê mas não escreve comentário: 403")
    void estagiarioNaoComenta() throws Exception {
        Atendimento atendimento = emRevisaoDoSeed();

        comentar(atendimento, token(ESTAGIARIO), "PLANO", "Já corrigi.")
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("comentar segue permitido depois de aprovado: o parecer não se fecha com o status")
    void comentarDepoisDeAprovado() throws Exception {
        Atendimento atendimento = emRevisaoDoSeed();

        mockMvc.perform(post("/api/atendimentos/{id}/avaliacao", atendimento.getId())
                        .header("Authorization", "Bearer " + token(SUPERVISOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resultado": "APROVADO",
                                 "itens": [{"criterio": "Conduta", "nota": 9}]}
                                """))
                .andExpect(status().isCreated());

        comentar(atendimento, token(SUPERVISOR), "METAS", "Boa definição de metas.")
                .andExpect(status().isCreated());
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    /** Atendimento já em revisão, com submetidoEm escolhido para a fila ordenar. */
    private Atendimento emRevisaoDesde(Paciente paciente, Usuario estagiario, Usuario supervisor,
                                       LocalDate dataConsulta, LocalDateTime submetidoEm) {
        return novoAtendimento(paciente, estagiario, supervisor, dataConsulta,
                StatusAtendimento.EM_REVISAO, submetidoEm);
    }

    /** O caminho real: preenchido e submetido pela API, pelo estagiário do seed. */
    private Atendimento emRevisaoDoSeed() throws Exception {
        String token = token(ESTAGIARIO);
        Usuario estagiario = usuarioPorEmail(ESTAGIARIO);

        Atendimento atendimento = novoAtendimento(novoPaciente(estagiario), estagiario,
                usuarioPorEmail(SUPERVISOR), LocalDate.of(2025, 4, 10), StatusAtendimento.RASCUNHO);

        preencherSecoesObrigatorias(atendimento.getId(), token);
        submeter(atendimento.getId(), token).andExpect(status().isOk());

        return atendimento;
    }

    private org.springframework.test.web.servlet.ResultActions comentar(
            Atendimento atendimento, String token, String secao, String texto) throws Exception {
        return mockMvc.perform(post("/api/atendimentos/{id}/comentarios", atendimento.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"secao": "%s", "texto": "%s"}
                        """.formatted(secao, texto)));
    }
}
