package br.edu.nutriclinica.controller;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.Medicamento;
import br.edu.nutriclinica.domain.Paciente;
import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.domain.enums.StatusAtendimento;
import br.edu.nutriclinica.repository.MedicamentoRepository;
import br.edu.nutriclinica.repository.RecordatorioItemRepository;
import br.edu.nutriclinica.repository.RecordatorioRefeicaoRepository;
import br.edu.nutriclinica.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * As seções 1:N do prontuário, substituídas inteiras por PUT.
 *
 * <p>O que se testa aqui não é "a lista enviada volta igual" — é que a
 * substituição preserva os ids do que continuou e apaga de verdade o que saiu,
 * inclusive os filhos.
 */
class ColecaoProntuarioControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MedicamentoRepository medicamentoRepository;

    @Autowired
    private RecordatorioRefeicaoRepository recordatorioRefeicaoRepository;

    @Autowired
    private RecordatorioItemRepository recordatorioItemRepository;

    // ------------------------------------------------------------------
    // Medicamentos
    // ------------------------------------------------------------------

    @Test
    @DisplayName("PUT substitui a coleção: insere sem id, atualiza com id, apaga o que não veio")
    void substituiPreservandoIds() throws Exception {
        Atendimento atendimento = novoAtendimentoEmRascunho();
        String token = token(ESTAGIARIO);

        String criados = mockMvc.perform(putSecao(atendimento, "medicamentos", token, """
                        [
                          {"tipo": "MEDICAMENTO", "nome": "Losartana", "dose": "50mg"},
                          {"tipo": "SUPLEMENTO",  "nome": "Vitamina D", "dose": "2000UI"},
                          {"tipo": "MEDICAMENTO", "nome": "Metformina", "dose": "850mg"}
                        ]
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andReturn().getResponse().getContentAsString();

        JsonNode lista = objectMapper.readTree(criados);
        long idLosartana = lista.get(0).get("id").asLong();
        long idVitaminaD = lista.get(1).get("id").asLong();
        long idMetformina = lista.get(2).get("id").asLong();

        // A losartana muda de dose e continua a mesma linha; a vitamina D some;
        // a metformina fica intacta; entra um item novo, sem id.
        mockMvc.perform(putSecao(atendimento, "medicamentos", token, """
                        [
                          {"id": %d, "tipo": "MEDICAMENTO", "nome": "Losartana", "dose": "100mg"},
                          {"id": %d, "tipo": "MEDICAMENTO", "nome": "Metformina", "dose": "850mg"},
                          {"tipo": "SUPLEMENTO", "nome": "Ômega 3", "dose": "1g"}
                        ]
                        """.formatted(idLosartana, idMetformina)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(idLosartana))
                .andExpect(jsonPath("$[0].dose").value("100mg"))
                .andExpect(jsonPath("$[1].id").value(idMetformina))
                .andExpect(jsonPath("$[2].nome").value("Ômega 3"));

        assertThat(medicamentoRepository.findById(idVitaminaD)).isEmpty();
        assertThat(medicamentoRepository.findByAtendimentoIdOrderByIdAsc(atendimento.getId()))
                .hasSize(3)
                .noneMatch(medicamento -> medicamento.getNome().equals("Vitamina D"));
    }

    @Test
    @DisplayName("PUT com lista vazia esvazia a seção")
    void listaVaziaEsvaziaASecao() throws Exception {
        Atendimento atendimento = novoAtendimentoEmRascunho();
        String token = token(ESTAGIARIO);

        mockMvc.perform(putSecao(atendimento, "metas", token,
                        "[{\"descricao\":\"Beber 2L de água por dia\",\"prazo\":\"CURTO\"}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(putSecao(atendimento, "metas", token, "[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Seção esvaziada some do prontuário, como se nunca tivesse sido preenchida.
        mockMvc.perform(get("/api/atendimentos/{id}", atendimento.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metas").doesNotExist());
    }

    @Test
    @DisplayName("id de outro atendimento não é sequestrado: vira registro novo")
    void idDeOutroAtendimentoViraRegistroNovo() throws Exception {
        Atendimento alvo = novoAtendimentoEmRascunho();
        Atendimento outro = novoAtendimentoEmRascunho();
        String token = token(ESTAGIARIO);

        String criados = mockMvc.perform(putSecao(outro, "medicamentos", token,
                        "[{\"tipo\":\"MEDICAMENTO\",\"nome\":\"Do outro atendimento\"}]"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long idAlheio = objectMapper.readTree(criados).get(0).get("id").asLong();

        mockMvc.perform(putSecao(alvo, "medicamentos", token,
                        "[{\"id\":%d,\"tipo\":\"MEDICAMENTO\",\"nome\":\"Sequestrado\"}]".formatted(idAlheio)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(not((int) idAlheio)));

        // A linha do outro atendimento continua intacta.
        assertThat(medicamentoRepository.findById(idAlheio)).get()
                .extracting(Medicamento::getNome)
                .isEqualTo("Do outro atendimento");
    }

    @Test
    @DisplayName("item inválido na lista vira 422, não 500")
    void itemInvalidoNaLista() throws Exception {
        Atendimento atendimento = novoAtendimentoEmRascunho();

        mockMvc.perform(putSecao(atendimento, "medicamentos", token(ESTAGIARIO),
                        "[{\"tipo\":\"MEDICAMENTO\",\"nome\":\"Losartana\"},{\"tipo\":\"SUPLEMENTO\"}]"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));

        assertThat(medicamentoRepository.findByAtendimentoIdOrderByIdAsc(atendimento.getId())).isEmpty();
    }

    // ------------------------------------------------------------------
    // Recordatório: dois níveis
    // ------------------------------------------------------------------

    @Test
    @DisplayName("remover uma refeição apaga os itens dela")
    void removerRefeicaoApagaOsItens() throws Exception {
        Atendimento atendimento = novoAtendimentoEmRascunho();
        String token = token(ESTAGIARIO);

        String criado = mockMvc.perform(putSecao(atendimento, "recordatorio", token, """
                        [
                          {
                            "tipoRefeicao": "DESJEJUM", "horario": "07:30", "localRefeicao": "Casa",
                            "itens": [
                              {"alimento": "Pão francês", "quantidade": "1 unidade"},
                              {"alimento": "Café com leite", "quantidade": "200ml"}
                            ]
                          },
                          {
                            "tipoRefeicao": "ALMOCO", "horario": "12:00",
                            "itens": [
                              {"alimento": "Arroz", "quantidade": "4 colheres"},
                              {"alimento": "Feijão", "quantidade": "1 concha"},
                              {"alimento": "Frango grelhado", "quantidade": "150g"}
                            ]
                          }
                        ]
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].horario").value("07:30"))
                .andExpect(jsonPath("$[0].itens.length()").value(2))
                .andExpect(jsonPath("$[1].itens.length()").value(3))
                .andReturn().getResponse().getContentAsString();

        JsonNode refeicoes = objectMapper.readTree(criado);
        long idDesjejum = refeicoes.get(0).get("id").asLong();
        long idAlmoco = refeicoes.get(1).get("id").asLong();
        List<Long> itensDoAlmoco = refeicoes.get(1).get("itens").findValues("id")
                .stream().map(JsonNode::asLong).toList();

        assertThat(itensDoAlmoco).hasSize(3);

        // O almoço sai do recordatório. Os três itens dele têm de sair junto.
        mockMvc.perform(putSecao(atendimento, "recordatorio", token, """
                        [
                          {
                            "id": %d, "tipoRefeicao": "DESJEJUM", "horario": "07:30",
                            "itens": [{"alimento": "Pão francês", "quantidade": "2 unidades"}]
                          }
                        ]
                        """.formatted(idDesjejum)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                // A refeição que continuou é a mesma linha.
                .andExpect(jsonPath("$[0].id").value(idDesjejum))
                .andExpect(jsonPath("$[0].itens.length()").value(1));

        assertThat(recordatorioRefeicaoRepository.findById(idAlmoco)).isEmpty();
        assertThat(recordatorioItemRepository.findByRefeicaoIdOrderByOrdemAsc(idAlmoco)).isEmpty();
        assertThat(recordatorioItemRepository.findAllById(itensDoAlmoco)).isEmpty();
    }

    @Test
    @DisplayName("a ordem enviada é respeitada na leitura; sem ordem, vale a posição na lista")
    void ordemDasRefeicoesEItens() throws Exception {
        Atendimento atendimento = novoAtendimentoEmRascunho();
        String token = token(ESTAGIARIO);

        // Enviadas fora de ordem, com ordem explícita invertendo a posição.
        mockMvc.perform(putSecao(atendimento, "recordatorio", token, """
                        [
                          {"tipoRefeicao": "JANTAR",   "ordem": 2, "itens": []},
                          {"tipoRefeicao": "DESJEJUM", "ordem": 1,
                           "itens": [
                             {"alimento": "Mamão",  "ordem": 2},
                             {"alimento": "Aveia",  "ordem": 1}
                           ]}
                        ]
                        """))
                .andExpect(status().isOk())
                // A resposta do PUT devolve na ordem em que o cliente enviou.
                .andExpect(jsonPath("$[0].tipoRefeicao").value("JANTAR"));

        // Já a leitura do prontuário ordena pelo campo ordem, nos dois níveis.
        mockMvc.perform(get("/api/atendimentos/{id}", atendimento.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordatorio[0].tipoRefeicao").value("DESJEJUM"))
                .andExpect(jsonPath("$.recordatorio[0].itens[0].alimento").value("Aveia"))
                .andExpect(jsonPath("$.recordatorio[0].itens[1].alimento").value("Mamão"))
                .andExpect(jsonPath("$.recordatorio[1].tipoRefeicao").value("JANTAR"));

        // Sem ordem no corpo, a posição na lista é que vale.
        mockMvc.perform(putSecao(atendimento, "recordatorio", token,
                        "[{\"tipoRefeicao\":\"CEIA\"},{\"tipoRefeicao\":\"ALMOCO\"}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ordem").value(0))
                .andExpect(jsonPath("$[1].ordem").value(1));
    }

    @Test
    @DisplayName("refeição sem itens no corpo fica sem itens, não com os anteriores")
    void refeicaoSemItensNoCorpo() throws Exception {
        Atendimento atendimento = novoAtendimentoEmRascunho();
        String token = token(ESTAGIARIO);

        String criado = mockMvc.perform(putSecao(atendimento, "recordatorio", token,
                        "[{\"tipoRefeicao\":\"CEIA\",\"itens\":[{\"alimento\":\"Iogurte\"}]}]"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long idCeia = objectMapper.readTree(criado).get(0).get("id").asLong();

        mockMvc.perform(putSecao(atendimento, "recordatorio", token,
                        "[{\"id\":%d,\"tipoRefeicao\":\"CEIA\"}]".formatted(idCeia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(idCeia))
                .andExpect(jsonPath("$[0].itens.length()").value(0));

        assertThat(recordatorioItemRepository.findByRefeicaoIdOrderByOrdemAsc(idCeia)).isEmpty();
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private Atendimento novoAtendimentoEmRascunho() {
        Usuario estagiario = usuarioPorEmail(ESTAGIARIO);
        Paciente paciente = novoPaciente(estagiario);
        return novoAtendimento(paciente, estagiario, usuarioPorEmail(SUPERVISOR),
                LocalDate.of(2025, 9, 9), StatusAtendimento.RASCUNHO);
    }

    private MockHttpServletRequestBuilder putSecao(
            Atendimento atendimento, String secao, String token, String corpo) {
        return put("/api/atendimentos/{id}/" + secao, atendimento.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo);
    }
}
