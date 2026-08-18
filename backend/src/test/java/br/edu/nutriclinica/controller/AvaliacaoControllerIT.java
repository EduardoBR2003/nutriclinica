package br.edu.nutriclinica.controller;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.AvaliacaoSupervisor;
import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.domain.enums.Perfil;
import br.edu.nutriclinica.domain.enums.StatusAtendimento;
import br.edu.nutriclinica.repository.AvaliacaoSupervisorRepository;
import br.edu.nutriclinica.repository.ItemRubricaRepository;
import br.edu.nutriclinica.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /api/atendimentos/{id}/avaliacao: quem avalia, em que estado, e a nota final
 * que só o servidor calcula.
 */
class AvaliacaoControllerIT extends AbstractIntegrationTest {

    @Autowired
    private AvaliacaoSupervisorRepository avaliacaoSupervisorRepository;

    @Autowired
    private ItemRubricaRepository itemRubricaRepository;

    // ------------------------------------------------------------------
    // Nota final
    // ------------------------------------------------------------------

    @Test
    @DisplayName("notaFinal ignora o valor forjado no corpo e usa a média ponderada")
    void notaFinalForjada() throws Exception {
        Atendimento atendimento = emRevisao();

        // (10×3 + 5×1) / 4 = 8.75, aconteça o que acontecer com o "notaFinal: 10".
        avaliar(atendimento, token(SUPERVISOR), """
                {"resultado": "APROVADO",
                 "notaFinal": 10,
                 "parecerGeral": "Boa evolução do raciocínio clínico.",
                 "itens": [
                   {"criterio": "Diagnóstico PES", "peso": 3, "nota": 10},
                   {"criterio": "Plano de intervenção", "peso": 1, "nota": 5}
                 ]}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notaFinal").value(8.75))
                .andExpect(jsonPath("$.resultado").value("APROVADO"))
                .andExpect(jsonPath("$.supervisor.email").value(SUPERVISOR))
                .andExpect(jsonPath("$.itens.length()").value(2));
    }

    @Test
    @DisplayName("peso ausente vale 1, como o default do contrato")
    void pesoAusenteValeUm() throws Exception {
        Atendimento atendimento = emRevisao();

        // (8 + 6 + 7) / 3 = 7.00
        avaliar(atendimento, token(SUPERVISOR), """
                {"resultado": "APROVADO",
                 "itens": [
                   {"criterio": "Anamnese", "nota": 8},
                   {"criterio": "Antropometria", "nota": 6},
                   {"criterio": "Conduta", "nota": 7}
                 ]}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notaFinal").value(7.00))
                .andExpect(jsonPath("$.itens[0].peso").value(1.00));
    }

    @Test
    @DisplayName("a média ponderada arredonda para duas casas, meio para cima")
    void arredondamento() throws Exception {
        Atendimento atendimento = emRevisao();

        // (8×2 + 6×1) / 3 = 7.333... → 7.33
        avaliar(atendimento, token(SUPERVISOR), """
                {"resultado": "APROVADO",
                 "itens": [
                   {"criterio": "Diagnóstico", "peso": 2, "nota": 8},
                   {"criterio": "Plano", "peso": 1, "nota": 6}
                 ]}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notaFinal").value(7.33));
    }

    @Test
    @DisplayName("peso zero é recusado na borda: 422")
    void pesoZero() throws Exception {
        Atendimento atendimento = emRevisao();

        avaliar(atendimento, token(SUPERVISOR), """
                {"resultado": "APROVADO",
                 "itens": [{"criterio": "Anamnese", "peso": 0, "nota": 8}]}
                """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    @Test
    @DisplayName("rubrica vazia é recusada: 422")
    void rubricaVazia() throws Exception {
        Atendimento atendimento = emRevisao();

        avaliar(atendimento, token(SUPERVISOR), """
                {"resultado": "APROVADO", "itens": []}
                """)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    // ------------------------------------------------------------------
    // Quem avalia, e quando
    // ------------------------------------------------------------------

    @Test
    @DisplayName("supervisor que orienta o estagiário mas não é o designado: 403")
    void supervisorNaoDesignado() throws Exception {
        Atendimento atendimento = emRevisao();

        // O vínculo é o que o faz enxergar o atendimento; sem ele a resposta
        // seria 404, e o teste estaria medindo outra coisa.
        Usuario outroSupervisor = novoUsuario("Prof. Suplente", Perfil.SUPERVISOR);
        vincular(outroSupervisor, usuarioPorEmail(ESTAGIARIO));

        avaliar(atendimento, token(outroSupervisor.getEmail()), rubricaSimples())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));

        assertThat(avaliacaoSupervisorRepository.existsByAtendimentoId(atendimento.getId())).isFalse();
    }

    @Test
    @DisplayName("supervisor sem vínculo nenhum recebe 404, não 403")
    void supervisorForaDoEscopo() throws Exception {
        Atendimento atendimento = emRevisao();
        Usuario estranho = novoUsuario("Prof. de Outro Curso", Perfil.SUPERVISOR);

        avaliar(atendimento, token(estranho.getEmail()), rubricaSimples())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("avaliar um atendimento em RASCUNHO: 409 TRANSICAO_INVALIDA")
    void avaliarRascunho() throws Exception {
        Usuario estagiario = usuarioPorEmail(ESTAGIARIO);
        Atendimento rascunho = novoAtendimento(novoPaciente(estagiario), estagiario,
                usuarioPorEmail(SUPERVISOR), LocalDate.of(2025, 7, 2), StatusAtendimento.RASCUNHO);

        avaliar(rascunho, token(SUPERVISOR), rubricaSimples())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICAO_INVALIDA"));

        // A recusa veio antes de qualquer escrita.
        assertThat(avaliacaoSupervisorRepository.existsByAtendimentoId(rascunho.getId())).isFalse();
    }

    @Test
    @DisplayName("estagiário não avalia o próprio prontuário: 403")
    void estagiarioNaoAvalia() throws Exception {
        Atendimento atendimento = emRevisao();

        avaliar(atendimento, token(ESTAGIARIO), rubricaSimples())
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // Leitura
    // ------------------------------------------------------------------

    @Test
    @DisplayName("estagiário lê a própria avaliação depois de aprovada")
    void estagiarioLeAvaliacao() throws Exception {
        Atendimento atendimento = emRevisao();
        avaliar(atendimento, token(SUPERVISOR), rubricaSimples()).andExpect(status().isCreated());

        mockMvc.perform(get("/api/atendimentos/{id}/avaliacao", atendimento.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notaFinal").value(9.00))
                .andExpect(jsonPath("$.resultado").value("APROVADO"));

        // E o prontuário completo traz a avaliação junto.
        mockMvc.perform(get("/api/atendimentos/{id}", atendimento.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avaliacao.notaFinal").value(9.00));
    }

    @Test
    @DisplayName("atendimento ainda não avaliado devolve 404 no GET da avaliação")
    void semAvaliacao() throws Exception {
        Atendimento atendimento = emRevisao();

        mockMvc.perform(get("/api/atendimentos/{id}/avaliacao", atendimento.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    // ------------------------------------------------------------------
    // Reavaliação
    // ------------------------------------------------------------------

    @Test
    @DisplayName("reavaliar substitui os itens da rubrica e mantém o id da avaliação")
    void reavaliar() throws Exception {
        String tokenSupervisor = token(SUPERVISOR);
        String tokenEstagiario = token(ESTAGIARIO);
        Atendimento atendimento = emRevisao();

        avaliar(atendimento, tokenSupervisor, """
                {"resultado": "DEVOLVIDO",
                 "itens": [
                   {"criterio": "Diagnóstico", "nota": 4},
                   {"criterio": "Plano", "nota": 5},
                   {"criterio": "Metas", "nota": 6}
                 ]}
                """)
                .andExpect(status().isCreated());

        Long idDaPrimeira = avaliacaoSupervisorRepository
                .findByAtendimentoId(atendimento.getId()).orElseThrow().getId();

        // O estagiário corrige e devolve para revisão.
        submeter(atendimento.getId(), tokenEstagiario).andExpect(status().isOk());

        avaliar(atendimento, tokenSupervisor, """
                {"resultado": "APROVADO",
                 "itens": [{"criterio": "Diagnóstico", "nota": 9}]}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notaFinal").value(9.00))
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].criterio").value("Diagnóstico"));

        AvaliacaoSupervisor avaliacao = avaliacaoSupervisorRepository
                .findByAtendimentoId(atendimento.getId()).orElseThrow();

        // A tabela tem UNIQUE(atendimento_id): a linha é reaproveitada, não trocada.
        assertThat(avaliacao.getId()).isEqualTo(idDaPrimeira);
        // Os itens vêm pelo repository: a coleção da entidade é LAZY, e fora da
        // transação do serviço ela não tem sessão para inicializar.
        assertThat(itemRubricaRepository.findByAvaliacaoIdOrderByOrdemAsc(avaliacao.getId()))
                .hasSize(1)
                .allSatisfy(item -> assertThat(item.getCriterio()).isEqualTo("Diagnóstico"));
        // Um relógio só para os dois carimbos.
        assertThat(avaliacao.getAvaliadoEm())
                .isEqualTo(atendimentoRepository.findById(atendimento.getId()).orElseThrow().getAvaliadoEm());
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    /** Atendimento do estagiário do seed, submetido pela API e aguardando parecer. */
    private Atendimento emRevisao() throws Exception {
        String token = token(ESTAGIARIO);
        Usuario estagiario = usuarioPorEmail(ESTAGIARIO);

        Atendimento atendimento = novoAtendimento(novoPaciente(estagiario), estagiario,
                usuarioPorEmail(SUPERVISOR), LocalDate.of(2025, 7, 1), StatusAtendimento.RASCUNHO);

        preencherSecoesObrigatorias(atendimento.getId(), token);
        submeter(atendimento.getId(), token).andExpect(status().isOk());

        return atendimento;
    }

    private String rubricaSimples() {
        return """
                {"resultado": "APROVADO",
                 "itens": [{"criterio": "Conduta nutricional", "nota": 9}]}
                """;
    }

    private ResultActions avaliar(Atendimento atendimento, String token, String corpo) throws Exception {
        return mockMvc.perform(post("/api/atendimentos/{id}/avaliacao", atendimento.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }
}
