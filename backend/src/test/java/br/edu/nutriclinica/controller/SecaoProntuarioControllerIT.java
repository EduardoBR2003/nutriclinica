package br.edu.nutriclinica.controller;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.Paciente;
import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.domain.Antropometria;
import br.edu.nutriclinica.domain.enums.Perfil;
import br.edu.nutriclinica.domain.enums.Sexo;
import br.edu.nutriclinica.domain.enums.StatusAtendimento;
import br.edu.nutriclinica.repository.AntropometriaRepository;
import br.edu.nutriclinica.repository.QueixaPrincipalRepository;
import br.edu.nutriclinica.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * As seções 1:1 do prontuário: a porta de escrita e a semântica do PATCH
 * parcial.
 *
 * <p>As regras de acesso são testadas aqui uma vez por resultado possível — 409,
 * 403, 404 — e não seção por seção: todas as onze passam pelo mesmo
 * {@code AtendimentoEditavelValidator}, e repetir onze vezes o mesmo cenário
 * testaria a fiação, não a regra. Que cada rota realmente passa por ele é o que
 * {@link #todasAsSecoesPassamPelaPortaDeEscrita} verifica.
 */
class SecaoProntuarioControllerIT extends AbstractIntegrationTest {

    /** As onze rotas do contrato, com um corpo mínimo válido para cada uma. */
    private static final String[][] SECOES = {
            {"PATCH", "queixa-principal", "{\"motivo\":\"x\"}"},
            {"PATCH", "historia-clinica", "{\"alergias\":\"x\"}"},
            {"PATCH", "antropometria", "{\"pesoKg\":70.00}"},
            {"PATCH", "frequencia-alimentar", "{\"frutas\":\"DIARIO\"}"},
            {"PATCH", "comportamento-alimentar", "{\"comeRapido\":true}"},
            {"PATCH", "diagnostico", "{\"problema\":\"x\"}"},
            {"PATCH", "plano", "{\"objetivos\":\"x\"}"},
            {"PUT", "medicamentos", "[{\"tipo\":\"MEDICAMENTO\",\"nome\":\"x\"}]"},
            {"PUT", "exames", "[{\"nomeExame\":\"x\"}]"},
            {"PUT", "recordatorio", "[{\"tipoRefeicao\":\"ALMOCO\"}]"},
            {"PUT", "metas", "[{\"descricao\":\"x\",\"prazo\":\"CURTO\"}]"},
    };

    @Autowired
    private QueixaPrincipalRepository queixaPrincipalRepository;

    @Autowired
    private AntropometriaRepository antropometriaRepository;

    // ------------------------------------------------------------------
    // A porta de escrita
    // ------------------------------------------------------------------

    @Test
    @DisplayName("PATCH em atendimento EM_REVISAO: 409 TRANSICAO_INVALIDA e nada gravado")
    void emRevisaoRecusaEscrita() throws Exception {
        Atendimento atendimento = atendimentoDoEstagiarioDoSeed(StatusAtendimento.EM_REVISAO);

        mockMvc.perform(requisicao("PATCH", atendimento, "queixa-principal",
                        token(ESTAGIARIO), "{\"motivo\":\"Quer emagrecer\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICAO_INVALIDA"));

        assertThat(queixaPrincipalRepository.findById(atendimento.getId())).isEmpty();
    }

    @Test
    @DisplayName("estagiário que não é o dono não escreve — e recebe 404, não 403")
    void estagiarioQueNaoEhDonoNaoEscreve() throws Exception {
        // Colega da mesma turma, orientado pelo mesmo supervisor: o prontuário
        // ainda assim não é dele.
        Usuario supervisor = usuarioPorEmail(SUPERVISOR);
        Usuario colega = novoUsuario("Colega de Turma", Perfil.ESTAGIARIO);
        vincular(supervisor, colega);

        Atendimento atendimento = atendimentoDoEstagiarioDoSeed(StatusAtendimento.RASCUNHO);

        // 404 e não 403 de propósito: para um estagiário, "não é meu" e "não
        // existe" são o mesmo fato — o escopo dele são só os próprios
        // atendimentos. Um 403 aqui confirmaria que o prontuário existe, que já
        // é informação sensível. O 403 fica para quem enxerga o atendimento mas
        // não pode escrever nele, caso do supervisor.
        mockMvc.perform(requisicao("PATCH", atendimento, "queixa-principal",
                        token(colega.getEmail()), "{\"motivo\":\"Quer emagrecer\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));

        assertThat(queixaPrincipalRepository.findById(atendimento.getId())).isEmpty();
    }

    @Test
    @DisplayName("atendimento fora do escopo do usuário: 404, não 403")
    void foraDoEscopoRecebe404() throws Exception {
        Usuario outroEstagiario = novoUsuario("Estagiário de Outra Turma", Perfil.ESTAGIARIO);
        Usuario outroSupervisor = novoUsuario("Prof. de Outra Turma", Perfil.SUPERVISOR);
        vincular(outroSupervisor, outroEstagiario);
        Paciente paciente = novoPaciente(outroEstagiario);
        Atendimento alheio = novoAtendimento(paciente, outroEstagiario, outroSupervisor,
                LocalDate.of(2025, 4, 4), StatusAtendimento.RASCUNHO);

        // 404 e não 403: confirmar que esse prontuário existe já seria vazamento.
        mockMvc.perform(requisicao("PATCH", alheio, "diagnostico",
                        token(ESTAGIARIO), "{\"problema\":\"x\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("todas as onze seções passam pela porta de escrita")
    void todasAsSecoesPassamPelaPortaDeEscrita() throws Exception {
        Atendimento aprovado = atendimentoDoEstagiarioDoSeed(StatusAtendimento.APROVADO);
        String token = token(ESTAGIARIO);

        for (String[] secao : SECOES) {
            mockMvc.perform(requisicao(secao[0], aprovado, secao[1], token, secao[2]))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("TRANSICAO_INVALIDA"));
        }
    }

    @Test
    @DisplayName("supervisor não escreve em nenhuma seção: 403")
    void supervisorNaoEscreve() throws Exception {
        Atendimento atendimento = atendimentoDoEstagiarioDoSeed(StatusAtendimento.RASCUNHO);
        String token = token(SUPERVISOR);

        for (String[] secao : SECOES) {
            mockMvc.perform(requisicao(secao[0], atendimento, secao[1], token, secao[2]))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));
        }
    }

    @Test
    @DisplayName("sem token não passa da cadeia de filtros: 401")
    void semToken() throws Exception {
        Atendimento atendimento = atendimentoDoEstagiarioDoSeed(StatusAtendimento.RASCUNHO);

        mockMvc.perform(patch("/api/atendimentos/{id}/plano", atendimento.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objetivos\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTORIZADO"));
    }

    @Test
    @DisplayName("DEVOLVIDO_PARA_CORRECAO aceita escrita e continua devolvido")
    void devolvidoAceitaEscritaSemMudarDeStatus() throws Exception {
        Atendimento atendimento = atendimentoDoEstagiarioDoSeed(StatusAtendimento.DEVOLVIDO_PARA_CORRECAO);

        mockMvc.perform(requisicao("PATCH", atendimento, "diagnostico",
                        token(ESTAGIARIO), "{\"problema\":\"Ingestão energética excessiva\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.problema").value("Ingestão energética excessiva"));

        // Corrigir não devolve o prontuário à fila: só o submeter faz isso.
        Atendimento recarregado = atendimentoRepository.findById(atendimento.getId()).orElseThrow();
        assertThat(recarregado.getStatus()).isEqualTo(StatusAtendimento.DEVOLVIDO_PARA_CORRECAO);
    }

    // ------------------------------------------------------------------
    // Semântica do PATCH parcial
    // ------------------------------------------------------------------

    @Test
    @DisplayName("campo ausente no corpo mantém o valor; campo null limpa")
    void patchParcialNaoApagaCamposAusentes() throws Exception {
        Atendimento atendimento = atendimentoDoEstagiarioDoSeed(StatusAtendimento.RASCUNHO);
        String token = token(ESTAGIARIO);

        mockMvc.perform(requisicao("PATCH", atendimento, "queixa-principal", token, """
                        {
                          "motivo": "Ganho de peso recente",
                          "objetivoConsulta": "Reeducação alimentar",
                          "tempoQueixa": "6 meses"
                        }
                        """))
                .andExpect(status().isOk());

        // A tela salva só o campo que o estagiário acabou de editar.
        mockMvc.perform(requisicao("PATCH", atendimento, "queixa-principal", token,
                        "{\"motivo\":\"Ganho de peso em 6 meses\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motivo").value("Ganho de peso em 6 meses"))
                .andExpect(jsonPath("$.objetivoConsulta").value("Reeducação alimentar"))
                .andExpect(jsonPath("$.tempoQueixa").value("6 meses"));

        // Já o null explícito é uma ordem de limpar — e só atinge quem foi enviado.
        mockMvc.perform(requisicao("PATCH", atendimento, "queixa-principal", token,
                        "{\"objetivoConsulta\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.objetivoConsulta").doesNotExist())
                .andExpect(jsonPath("$.motivo").value("Ganho de peso em 6 meses"))
                .andExpect(jsonPath("$.tempoQueixa").value("6 meses"));

        assertThat(queixaPrincipalRepository.findById(atendimento.getId()).orElseThrow()
                .getObjetivoConsulta()).isNull();
    }

    @Test
    @DisplayName("booleano ausente é mantido; booleano null volta a ser 'não perguntei'")
    void patchParcialDeBooleanos() throws Exception {
        Atendimento atendimento = atendimentoDoEstagiarioDoSeed(StatusAtendimento.RASCUNHO);
        String token = token(ESTAGIARIO);

        mockMvc.perform(requisicao("PATCH", atendimento, "comportamento-alimentar", token,
                        "{\"comeRapido\":true,\"pulaRefeicoes\":false,\"compulsaoAlimentar\":true}"))
                .andExpect(status().isOk());

        // false é um valor, não uma ausência: não pode ser confundido com "limpar".
        mockMvc.perform(requisicao("PATCH", atendimento, "comportamento-alimentar", token,
                        "{\"compulsaoAlimentar\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comeRapido").value(true))
                .andExpect(jsonPath("$.pulaRefeicoes").value(false))
                .andExpect(jsonPath("$.compulsaoAlimentar").doesNotExist());
    }

    @Test
    @DisplayName("antropometria: imc forjado no corpo é descartado e o calculado é gravado")
    void antropometriaIgnoraImcForjado() throws Exception {
        Usuario estagiario = usuarioPorEmail(ESTAGIARIO);
        Paciente paciente = novoPaciente("Paciente Antropometria",
                Sexo.FEMININO, LocalDate.of(1990, 1, 1), estagiario);
        Atendimento atendimento = novoAtendimento(paciente, estagiario, usuarioPorEmail(SUPERVISOR),
                LocalDate.of(2025, 1, 1), StatusAtendimento.RASCUNHO);

        // 60 kg e 1,60 m => IMC 23,44 / eutrofia. O cliente tenta impor outra coisa.
        mockMvc.perform(requisicao("PATCH", atendimento, "antropometria", token(ESTAGIARIO), """
                        {
                          "pesoKg": 60.00,
                          "alturaCm": 160.0,
                          "circCinturaCm": 70.0,
                          "circQuadrilCm": 100.0,
                          "imc": 99.99,
                          "classificacaoImc": "OBESIDADE_GRAU_III",
                          "relacaoCinturaQuadril": 5.00,
                          "rcqElevada": true,
                          "riscoCardiovascular": "MUITO_AUMENTADO"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imc").value(23.44))
                .andExpect(jsonPath("$.classificacaoImc").value("EUTROFIA"))
                .andExpect(jsonPath("$.relacaoCinturaQuadril").value(0.70))
                .andExpect(jsonPath("$.rcqElevada").value(false));

        // O que foi ao banco é o cálculo do servidor, não o que o cliente mandou.
        Antropometria gravada = antropometriaRepository.findById(atendimento.getId()).orElseThrow();
        assertThat(gravada.getImc()).isEqualByComparingTo("23.44");
        assertThat(gravada.getClassificacaoImc()).isEqualTo("EUTROFIA");
        assertThat(gravada.getRelacaoCinturaQuadril()).isEqualByComparingTo("0.70");
    }

    @Test
    @DisplayName("valor fora da faixa vira 422 no schema Erro, não erro de constraint")
    void valorForaDaFaixa() throws Exception {
        Atendimento atendimento = atendimentoDoEstagiarioDoSeed(StatusAtendimento.RASCUNHO);

        mockMvc.perform(requisicao("PATCH", atendimento, "frequencia-alimentar",
                        token(ESTAGIARIO), "{\"aguaMlDia\":99999}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos[0].campo").value("aguaMlDia"));
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private Atendimento atendimentoDoEstagiarioDoSeed(StatusAtendimento status) {
        Usuario estagiario = usuarioPorEmail(ESTAGIARIO);
        Paciente paciente = novoPaciente(estagiario);
        return novoAtendimento(paciente, estagiario, usuarioPorEmail(SUPERVISOR),
                LocalDate.of(2025, 7, 7), status);
    }

    private RequestBuilder requisicao(String metodo,
                                      Atendimento atendimento,
                                      String secao,
                                      String token,
                                      String corpo) {
        String url = "/api/atendimentos/" + atendimento.getId() + "/" + secao;
        MockHttpServletRequestBuilder builder = "PUT".equals(metodo) ? put(url) : patch(url);

        return builder
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo);
    }
}
