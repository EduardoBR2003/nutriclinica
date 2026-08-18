package br.edu.nutriclinica.controller;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.domain.enums.StatusAtendimento;
import br.edu.nutriclinica.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O ciclo inteiro do prontuário, do rascunho à aprovação, passando por uma
 * devolução — a história que o sistema existe para contar.
 *
 * <p>Vive sozinho numa classe porque é um teste narrativo: cada passo depende do
 * anterior, e o que ele prova não é um endpoint, é a coerência entre eles. A
 * cada etapa confere as duas coisas que o estagiário vê na tela: o {@code status}
 * e o {@code editavel}.
 */
class CicloAtendimentoIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("rascunho → submeter → devolver → editar → submeter → aprovar")
    void cicloCompleto() throws Exception {
        String estagiario = token(ESTAGIARIO);
        String supervisor = token(SUPERVISOR);
        Atendimento atendimento = rascunho();

        // 1. Rascunho: o estagiário escreve.
        conferir(atendimento, estagiario, StatusAtendimento.RASCUNHO, true);
        preencherSecoesObrigatorias(atendimento.getId(), estagiario);

        // 2. Submetido: sai da mão do estagiário e entra na fila do supervisor.
        submeter(atendimento.getId(), estagiario)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_REVISAO"));
        conferir(atendimento, estagiario, StatusAtendimento.EM_REVISAO, false);
        editarPlano(atendimento, estagiario)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICAO_INVALIDA"));

        // 3. Devolvido: a permissão de edição volta, sem que ninguém a devolva
        //    explicitamente — ela é função do status.
        avaliar(atendimento, supervisor, "DEVOLVIDO", 5)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado").value("DEVOLVIDO"))
                .andExpect(jsonPath("$.notaFinal").value(5.00));
        conferir(atendimento, estagiario, StatusAtendimento.DEVOLVIDO_PARA_CORRECAO, true);

        // 4. Corrigindo: escrever numa seção não muda o status. O prontuário
        //    continua devolvido até o estagiário submeter de novo.
        editarPlano(atendimento, estagiario).andExpect(status().isOk());
        conferir(atendimento, estagiario, StatusAtendimento.DEVOLVIDO_PARA_CORRECAO, true);

        // 5. Ressubmetido.
        submeter(atendimento.getId(), estagiario).andExpect(status().isOk());
        conferir(atendimento, estagiario, StatusAtendimento.EM_REVISAO, false);

        // 6. Aprovado: fim de linha. Nem o estagiário escreve, nem se submete de novo.
        avaliar(atendimento, supervisor, "APROVADO", 9)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notaFinal").value(9.00));
        conferir(atendimento, estagiario, StatusAtendimento.APROVADO, false);

        editarPlano(atendimento, estagiario).andExpect(status().isConflict());
        submeter(atendimento.getId(), estagiario)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICAO_INVALIDA"));

        // O prontuário aprovado guarda os dois carimbos e o parecer final.
        mockMvc.perform(get("/api/atendimentos/{id}", atendimento.getId())
                        .header("Authorization", "Bearer " + estagiario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submetidoEm").exists())
                .andExpect(jsonPath("$.avaliadoEm").exists())
                .andExpect(jsonPath("$.avaliacao.resultado").value("APROVADO"))
                .andExpect(jsonPath("$.avaliacao.notaFinal").value(9.00));
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private void conferir(Atendimento atendimento, String token,
                          StatusAtendimento status, boolean editavel) throws Exception {
        mockMvc.perform(get("/api/atendimentos/{id}", atendimento.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(status.name()))
                .andExpect(jsonPath("$.editavel").value(editavel));
    }

    private org.springframework.test.web.servlet.ResultActions editarPlano(
            Atendimento atendimento, String token) throws Exception {
        return mockMvc.perform(patch("/api/atendimentos/{id}/plano", atendimento.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"objetivos": "Reduzir 3 kg em dois meses, conforme orientação do supervisor"}
                        """));
    }

    private org.springframework.test.web.servlet.ResultActions avaliar(
            Atendimento atendimento, String token, String resultado, int nota) throws Exception {
        return mockMvc.perform(post("/api/atendimentos/{id}/avaliacao", atendimento.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"resultado": "%s",
                         "parecerGeral": "Parecer do ciclo de teste.",
                         "itens": [{"criterio": "Raciocínio clínico", "nota": %d}]}
                        """.formatted(resultado, nota)));
    }

    private Atendimento rascunho() {
        Usuario estagiario = usuarioPorEmail(ESTAGIARIO);
        return novoAtendimento(novoPaciente(estagiario), estagiario, usuarioPorEmail(SUPERVISOR),
                LocalDate.of(2025, 10, 1), StatusAtendimento.RASCUNHO);
    }
}
