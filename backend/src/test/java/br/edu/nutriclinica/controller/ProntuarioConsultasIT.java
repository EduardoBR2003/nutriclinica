package br.edu.nutriclinica.controller;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.Paciente;
import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.domain.enums.StatusAtendimento;
import br.edu.nutriclinica.support.AbstractIntegrationTest;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O custo de montar o prontuário completo não pode depender do tamanho dele.
 *
 * <p>Um recordatório de 24 horas tem seis refeições com vários itens cada, e o
 * caminho ingênuo — uma consulta por refeição para buscar os itens — só aparece
 * em produção, quando o prontuário já está cheio. Este teste compara o número de
 * consultas de um prontuário mínimo com o de um prontuário grande: se a diferença
 * não for zero, o N+1 voltou.
 */
class ProntuarioConsultasIT extends AbstractIntegrationTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    @DisplayName("montar o prontuário custa o mesmo com 1 e com 6 refeições")
    void custoConstante() throws Exception {
        String token = token(ESTAGIARIO);

        Atendimento pequeno = comRecordatorio(token, 1, 1);
        Atendimento grande = comRecordatorio(token, 6, 5);

        long consultasPequeno = consultasDoDetalhe(pequeno, token);
        long consultasGrande = consultasDoDetalhe(grande, token);

        // Sem isto o teste passaria à toa caso o contador fosse desligado.
        assertThat(consultasPequeno)
                .as("o contador de consultas precisa estar ligado (generate_statistics)")
                .isPositive();

        assertThat(consultasGrande)
                .as("30 itens em 6 refeições não podem custar mais consultas que 1 item em 1 refeição")
                .isEqualTo(consultasPequeno);

    }

    /** Executa o GET e devolve quantas consultas ele disparou. */
    private long consultasDoDetalhe(Atendimento atendimento, String token) throws Exception {
        Statistics estatisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        estatisticas.clear();

        mockMvc.perform(get("/api/atendimentos/{id}", atendimento.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        return estatisticas.getPrepareStatementCount();
    }

    private Atendimento comRecordatorio(String token, int refeicoes, int itensPorRefeicao) throws Exception {
        Usuario estagiario = usuarioPorEmail(ESTAGIARIO);
        Paciente paciente = novoPaciente(estagiario);
        Atendimento atendimento = novoAtendimento(paciente, estagiario, usuarioPorEmail(SUPERVISOR),
                LocalDate.of(2025, 10, 10), StatusAtendimento.RASCUNHO);

        mockMvc.perform(put("/api/atendimentos/{id}/recordatorio", atendimento.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoRecordatorio(refeicoes, itensPorRefeicao)))
                .andExpect(status().isOk());

        return atendimento;
    }

    private String corpoRecordatorio(int refeicoes, int itensPorRefeicao) {
        return IntStream.range(0, refeicoes)
                .mapToObj(indice -> """
                        {"tipoRefeicao": "OUTRO", "ordem": %d, "itens": [%s]}
                        """.formatted(indice, itens(itensPorRefeicao)))
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String itens(int quantidade) {
        return IntStream.range(0, quantidade)
                .mapToObj(indice -> "{\"alimento\": \"Alimento %d\", \"ordem\": %d}".formatted(indice, indice))
                .collect(Collectors.joining(","));
    }
}
