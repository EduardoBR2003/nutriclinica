package br.edu.nutriclinica.controller;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.domain.enums.Perfil;
import br.edu.nutriclinica.domain.enums.StatusAtendimento;
import br.edu.nutriclinica.repository.LogAuditoriaRepository;
import br.edu.nutriclinica.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /api/atendimentos/{id}/submeter: seções obrigatórias, transição e trilha
 * de auditoria.
 */
class SubmissaoAtendimentoIT extends AbstractIntegrationTest {

    @Autowired
    private LogAuditoriaRepository logAuditoriaRepository;

    // ------------------------------------------------------------------
    // Seções obrigatórias
    // ------------------------------------------------------------------

    @Test
    @DisplayName("submeter sem diagnóstico: 409 SECOES_INCOMPLETAS apontando os três campos do PES")
    void semDiagnostico() throws Exception {
        String token = token(ESTAGIARIO);
        Atendimento atendimento = rascunhoDoSeed();

        // Tudo menos o diagnóstico.
        preencherQueixa(atendimento.getId(), token);
        preencherAntropometria(atendimento.getId(), token);
        preencherRecordatorio(atendimento.getId(), token);
        preencherPlano(atendimento.getId(), token);

        submeter(atendimento.getId(), token)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SECOES_INCOMPLETAS"))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("diagnostico.problema")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("diagnostico.etiologia")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("diagnostico.sinaisSintomas")))
                // O que estava preenchido não vira pendência.
                .andExpect(jsonPath("$.campos.length()").value(3));

        // A submissão recusada não mexeu no status.
        assertThat(atendimentoRepository.findById(atendimento.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusAtendimento.RASCUNHO);
    }

    @Test
    @DisplayName("prontuário vazio: cada campo obrigatório de cada seção vira uma pendência")
    void prontuarioVazio() throws Exception {
        Atendimento atendimento = rascunhoDoSeed();

        submeter(atendimento.getId(), token(ESTAGIARIO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SECOES_INCOMPLETAS"))
                .andExpect(jsonPath("$.campos.length()").value(8))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("queixaPrincipal.motivo")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("antropometria.pesoKg")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("antropometria.alturaCm")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("recordatorio")))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("plano.objetivos")));
    }

    @Test
    @DisplayName("seção criada com todos os campos nulos não conta como preenchida")
    void secaoEmBrancoNaoConta() throws Exception {
        String token = token(ESTAGIARIO);
        Atendimento atendimento = rascunhoDoSeed();

        preencherSecoesObrigatorias(atendimento.getId(), token);
        // O PATCH grava a linha mesmo com o campo apagado — é o que faz o
        // autosave funcionar, e é justamente a armadilha que o portão pega.
        secaoEmBranco(atendimento.getId(), token);

        submeter(atendimento.getId(), token)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SECOES_INCOMPLETAS"))
                .andExpect(jsonPath("$.campos[*].campo", hasItem("plano.objetivos")));
    }

    // ------------------------------------------------------------------
    // Transição
    // ------------------------------------------------------------------

    @Test
    @DisplayName("prontuário completo vai para EM_REVISAO, carimba submetidoEm e tira a edição")
    void submissaoCompleta() throws Exception {
        String token = token(ESTAGIARIO);
        Atendimento atendimento = rascunhoDoSeed();
        preencherSecoesObrigatorias(atendimento.getId(), token);

        submeter(atendimento.getId(), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_REVISAO"))
                .andExpect(jsonPath("$.editavel").value(false))
                .andExpect(jsonPath("$.submetidoEm").exists());

        Atendimento salvo = atendimentoRepository.findById(atendimento.getId()).orElseThrow();
        assertThat(salvo.getStatus()).isEqualTo(StatusAtendimento.EM_REVISAO);
        assertThat(salvo.getSubmetidoEm()).isNotNull();
    }

    @Test
    @DisplayName("submeter um atendimento já EM_REVISAO: 409 TRANSICAO_INVALIDA")
    void jaEmRevisao() throws Exception {
        String token = token(ESTAGIARIO);
        Atendimento atendimento = rascunhoDoSeed();
        preencherSecoesObrigatorias(atendimento.getId(), token);
        submeter(atendimento.getId(), token).andExpect(status().isOk());

        // O status é o erro mais fundamental: mesmo que faltasse conteúdo, é ele
        // que a resposta precisa descrever.
        submeter(atendimento.getId(), token)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICAO_INVALIDA"))
                .andExpect(jsonPath("$.campos").doesNotExist());
    }

    @Test
    @DisplayName("submeter um atendimento APROVADO: 409 TRANSICAO_INVALIDA")
    void jaAprovado() throws Exception {
        Usuario estagiario = usuarioPorEmail(ESTAGIARIO);
        Atendimento atendimento = novoAtendimento(novoPaciente(estagiario), estagiario,
                usuarioPorEmail(SUPERVISOR), LocalDate.of(2025, 6, 10), StatusAtendimento.APROVADO);

        submeter(atendimento.getId(), token(ESTAGIARIO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICAO_INVALIDA"));
    }

    // ------------------------------------------------------------------
    // Quem pode submeter
    // ------------------------------------------------------------------

    @Test
    @DisplayName("colega do mesmo supervisor não enxerga o prontuário alheio: 404")
    void colegaNaoSubmete() throws Exception {
        Atendimento atendimento = rascunhoDoSeed();
        preencherSecoesObrigatorias(atendimento.getId(), token(ESTAGIARIO));

        Usuario colega = novoUsuario("Colega de Turma", Perfil.ESTAGIARIO);
        vincular(usuarioPorEmail(SUPERVISOR), colega);

        // O escopo do estagiário são os próprios atendimentos e mais nada — nem
        // os dos colegas de orientação. Por isso 404 e não 403: confirmar que
        // aquele id existe já seria vazar prontuário alheio.
        submeter(atendimento.getId(), token(colega.getEmail()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("atendimento de outra turma devolve 404, não 403")
    void foraDoEscopo() throws Exception {
        Usuario outroEstagiario = novoUsuario("Estagiário de Outra Turma", Perfil.ESTAGIARIO);
        Usuario outroSupervisor = novoUsuario("Supervisor de Outra Turma", Perfil.SUPERVISOR);
        vincular(outroSupervisor, outroEstagiario);

        Atendimento alheio = novoAtendimento(novoPaciente(outroEstagiario), outroEstagiario,
                outroSupervisor, LocalDate.of(2025, 6, 11), StatusAtendimento.RASCUNHO);

        submeter(alheio.getId(), token(ESTAGIARIO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("supervisor não submete pelo estagiário: 403")
    void supervisorNaoSubmete() throws Exception {
        Atendimento atendimento = rascunhoDoSeed();
        // De propósito sem preencher: o 403 precisa vir antes do 409 de seções.

        // O supervisor designado enxerga o atendimento, então aqui o 403 é
        // legítimo: recusar a escrita não revela nada que ele já não visse.
        submeter(atendimento.getId(), token(SUPERVISOR))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"))
                // O 403 vem antes da checagem de conteúdo: quais seções estão
                // vazias é informação clínica, e não vaza para quem não é o dono.
                .andExpect(jsonPath("$.campos").doesNotExist());
    }

    // ------------------------------------------------------------------
    // Auditoria
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a submissão e a leitura do prontuário deixam rastro em log_auditoria")
    void deixaRastro() throws Exception {
        String token = token(ESTAGIARIO);
        Atendimento atendimento = rascunhoDoSeed();
        preencherSecoesObrigatorias(atendimento.getId(), token);
        submeter(atendimento.getId(), token).andExpect(status().isOk());

        mockMvc.perform(get("/api/atendimentos/{id}", atendimento.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Escopado pelo id do atendimento: o banco é compartilhado entre as
        // classes de teste, então contagem total não diria nada.
        var trilha = logAuditoriaRepository.findByEntidadeAndEntidadeIdOrderByCriadoEmDesc(
                "Atendimento", atendimento.getId(), PageRequest.of(0, 10)).getContent();

        assertThat(trilha).extracting("acao").contains("SUBMISSAO", "LEITURA_PRONTUARIO");
        assertThat(trilha).extracting("entidadeId").containsOnly(atendimento.getId());

        // Quem agiu, conferido pelo outro índice da trilha — o usuário é LAZY na
        // entidade e não se deixa navegar fora da transação.
        var doEstagiario = logAuditoriaRepository.findByUsuarioIdOrderByCriadoEmDesc(
                usuarioPorEmail(ESTAGIARIO).getId(), PageRequest.of(0, 50)).getContent();

        assertThat(doEstagiario)
                .filteredOn(registro -> atendimento.getId().equals(registro.getEntidadeId()))
                .extracting("acao")
                .contains("SUBMISSAO", "LEITURA_PRONTUARIO");
    }

    @Test
    @DisplayName("submissão recusada não deixa rastro: @AfterReturning só audita o que deu certo")
    void recusaNaoAudita() throws Exception {
        Atendimento atendimento = rascunhoDoSeed();

        submeter(atendimento.getId(), token(ESTAGIARIO))
                .andExpect(status().isConflict());

        var trilha = logAuditoriaRepository.findByEntidadeAndEntidadeIdOrderByCriadoEmDesc(
                "Atendimento", atendimento.getId(), PageRequest.of(0, 10)).getContent();

        assertThat(trilha).extracting("acao").doesNotContain("SUBMISSAO");
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    /** Rascunho do estagiário do seed, com o supervisor que já o orienta. */
    private Atendimento rascunhoDoSeed() {
        Usuario estagiario = usuarioPorEmail(ESTAGIARIO);
        return novoAtendimento(novoPaciente(estagiario), estagiario, usuarioPorEmail(SUPERVISOR),
                LocalDate.of(2025, 6, 1), StatusAtendimento.RASCUNHO);
    }

    /** Apaga os objetivos do plano — a linha continua lá, o conteúdo não. */
    private void secaoEmBranco(Long atendimentoId, String token) throws Exception {
        mockMvc.perform(patch("/api/atendimentos/{id}/plano", atendimentoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objetivos\": \"   \"}"))
                .andExpect(status().isOk());
    }
}
