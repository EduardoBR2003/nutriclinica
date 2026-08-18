package br.edu.nutriclinica.controller;

import br.edu.nutriclinica.domain.Antropometria;
import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.Paciente;
import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.domain.enums.Perfil;
import br.edu.nutriclinica.domain.enums.Sexo;
import br.edu.nutriclinica.domain.enums.StatusAtendimento;
import br.edu.nutriclinica.repository.AntropometriaRepository;
import br.edu.nutriclinica.repository.AtendimentoRepository;
import br.edu.nutriclinica.repository.PacienteRepository;
import br.edu.nutriclinica.repository.UsuarioRepository;
import br.edu.nutriclinica.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** PATCH /api/atendimentos/{id}/antropometria sobre os usuários do V2__seed.sql. */
class AntropometriaControllerIT extends AbstractIntegrationTest {

    private static final String ESTAGIARIO = "estagiario@nutriclinica.edu.br";
    private static final String SUPERVISOR = "supervisor@nutriclinica.edu.br";
    private static final String SENHA = "senha123";

    /** Mantém numero_prontuario único sem depender da ordem dos testes. */
    private static final AtomicInteger SEQUENCIA = new AtomicInteger();

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PacienteRepository pacienteRepository;
    @Autowired
    private AtendimentoRepository atendimentoRepository;
    @Autowired
    private AntropometriaRepository antropometriaRepository;

    @Test
    @DisplayName("salva os valores medidos e devolve os indicadores calculados pelo servidor")
    void salvaECalcula() throws Exception {
        Atendimento atendimento = novoAtendimento(
                Sexo.MASCULINO, LocalDate.of(1980, 3, 10), LocalDate.of(2025, 3, 10), StatusAtendimento.RASCUNHO);

        // 95 kg e 1,75 m => IMC 31,02 (obesidade grau I aos 45 anos).
        // Cintura 104 / quadril 110 => RCQ 0,95 (elevada) e risco muito aumentado.
        mockMvc.perform(patch("/api/atendimentos/{id}/antropometria", atendimento.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pesoKg": 95.00,
                                  "alturaCm": 175.0,
                                  "circCinturaCm": 104.0,
                                  "circQuadrilCm": 110.0,
                                  "aferidoEm": "2025-03-10"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imc").value(31.02))
                .andExpect(jsonPath("$.classificacaoImc").value("OBESIDADE_GRAU_I"))
                .andExpect(jsonPath("$.relacaoCinturaQuadril").value(0.95))
                .andExpect(jsonPath("$.rcqElevada").value(true))
                .andExpect(jsonPath("$.riscoCardiovascular").value("MUITO_AUMENTADO"));
    }

    @Test
    @DisplayName("valores calculados enviados no corpo são descartados")
    void descartaCalculadosDoCliente() throws Exception {
        Atendimento atendimento = novoAtendimento(
                Sexo.FEMININO, LocalDate.of(1990, 1, 1), LocalDate.of(2025, 1, 1), StatusAtendimento.RASCUNHO);

        // 60 kg e 1,60 m => IMC 23,44 / eutrofia. O cliente tenta impor outra coisa.
        mockMvc.perform(patch("/api/atendimentos/{id}/antropometria", atendimento.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pesoKg": 60.00,
                                  "alturaCm": 160.0,
                                  "circCinturaCm": 70.0,
                                  "circQuadrilCm": 100.0,
                                  "imc": 99.99,
                                  "classificacaoImc": "BAIXO_PESO",
                                  "relacaoCinturaQuadril": 5.00,
                                  "rcqElevada": true,
                                  "riscoCardiovascular": "MUITO_AUMENTADO"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imc").value(23.44))
                .andExpect(jsonPath("$.classificacaoImc").value("EUTROFIA"))
                .andExpect(jsonPath("$.relacaoCinturaQuadril").value(0.70))
                .andExpect(jsonPath("$.rcqElevada").value(false))
                .andExpect(jsonPath("$.riscoCardiovascular").value("BAIXO"));

        // O que foi ao banco é o cálculo do servidor, não o que o cliente mandou.
        Antropometria gravada = antropometriaRepository.findById(atendimento.getId()).orElseThrow();
        assertThat(gravada.getImc()).isEqualByComparingTo("23.44");
        assertThat(gravada.getClassificacaoImc()).isEqualTo("EUTROFIA");
        assertThat(gravada.getRelacaoCinturaQuadril()).isEqualByComparingTo("0.70");
    }

    @Test
    @DisplayName("risco cardiovascular não é persistido — é derivado na resposta")
    void riscoNaoEhPersistido() throws Exception {
        Atendimento atendimento = novoAtendimento(
                Sexo.MASCULINO, LocalDate.of(1985, 6, 1), LocalDate.of(2025, 6, 1), StatusAtendimento.RASCUNHO);

        mockMvc.perform(patch("/api/atendimentos/{id}/antropometria", atendimento.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pesoKg\":90.00,\"alturaCm\":180.0,\"circCinturaCm\":105.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riscoCardiovascular").value("MUITO_AUMENTADO"));

        Antropometria gravada = antropometriaRepository.findById(atendimento.getId()).orElseThrow();
        assertThat(gravada.getRiscoCardiovascular()).isNull();
        // Sem quadril não há RCQ, mas o IMC e o risco continuam saindo.
        assertThat(gravada.getRelacaoCinturaQuadril()).isNull();
        assertThat(gravada.getImc()).isEqualByComparingTo("27.78");
    }

    @Test
    @DisplayName("a idade é a da data da consulta, não a de hoje")
    void idadeNaDataDaConsulta() throws Exception {
        // Nasceu em 1965. Na consulta de 2025-01-01 tinha 59 anos (faz 60 em junho):
        // IMC 21,26 é EUTROFIA pela OMS, mas seria BAIXO_PESO por Lipschitz.
        Atendimento atendimento = novoAtendimento(
                Sexo.FEMININO, LocalDate.of(1965, 6, 15), LocalDate.of(2025, 1, 1), StatusAtendimento.RASCUNHO);

        mockMvc.perform(patch("/api/atendimentos/{id}/antropometria", atendimento.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pesoKg\":58.00,\"alturaCm\":165.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imc").value(21.30))
                .andExpect(jsonPath("$.classificacaoImc").value("EUTROFIA"));

        // Mesmo paciente, consulta depois do aniversário de 60: agora é Lipschitz.
        Atendimento depois = novoAtendimento(
                Sexo.FEMININO, LocalDate.of(1965, 6, 15), LocalDate.of(2025, 7, 1), StatusAtendimento.RASCUNHO);

        mockMvc.perform(patch("/api/atendimentos/{id}/antropometria", depois.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pesoKg\":58.00,\"alturaCm\":165.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classificacaoImc").value("BAIXO_PESO"));
    }

    @Test
    @DisplayName("sexo OUTRO não recebe rcqElevada nem risco cardiovascular")
    void sexoSemPontoDeCorte() throws Exception {
        Atendimento atendimento = novoAtendimento(
                Sexo.OUTRO, LocalDate.of(1995, 2, 2), LocalDate.of(2025, 2, 2), StatusAtendimento.RASCUNHO);

        mockMvc.perform(patch("/api/atendimentos/{id}/antropometria", atendimento.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pesoKg\":70.00,\"alturaCm\":175.0,\"circCinturaCm\":100.0,\"circQuadrilCm\":100.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imc").value(22.86))
                .andExpect(jsonPath("$.relacaoCinturaQuadril").value(1.00))
                .andExpect(jsonPath("$.rcqElevada").doesNotExist())
                .andExpect(jsonPath("$.riscoCardiovascular").doesNotExist());
    }

    @Test
    @DisplayName("prontuário EM_REVISAO é somente leitura: 409 TRANSICAO_INVALIDA")
    void emRevisaoNaoAceitaEscrita() throws Exception {
        Atendimento atendimento = novoAtendimento(
                Sexo.MASCULINO, LocalDate.of(1990, 1, 1), LocalDate.of(2025, 1, 1), StatusAtendimento.EM_REVISAO);

        mockMvc.perform(patch("/api/atendimentos/{id}/antropometria", atendimento.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pesoKg\":80.00,\"alturaCm\":180.0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICAO_INVALIDA"));

        assertThat(antropometriaRepository.findById(atendimento.getId())).isEmpty();
    }

    @Test
    @DisplayName("DEVOLVIDO_PARA_CORRECAO volta a aceitar escrita")
    void devolvidoAceitaEscrita() throws Exception {
        Atendimento atendimento = novoAtendimento(
                Sexo.MASCULINO, LocalDate.of(1990, 1, 1), LocalDate.of(2025, 1, 1),
                StatusAtendimento.DEVOLVIDO_PARA_CORRECAO);

        mockMvc.perform(patch("/api/atendimentos/{id}/antropometria", atendimento.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pesoKg\":80.00,\"alturaCm\":180.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imc").value(24.69));
    }

    @Test
    @DisplayName("APROVADO é somente leitura: 409 TRANSICAO_INVALIDA")
    void aprovadoNaoAceitaEscrita() throws Exception {
        Atendimento atendimento = novoAtendimento(
                Sexo.MASCULINO, LocalDate.of(1990, 1, 1), LocalDate.of(2025, 1, 1), StatusAtendimento.APROVADO);

        mockMvc.perform(patch("/api/atendimentos/{id}/antropometria", atendimento.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pesoKg\":80.00,\"alturaCm\":180.0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICAO_INVALIDA"));
    }

    @Test
    @DisplayName("estagiário não vê nem escreve em atendimento de outro: 403")
    void atendimentoDeOutroEstagiario() throws Exception {
        Usuario outro = novoEstagiario();
        Atendimento atendimento = novoAtendimento(
                Sexo.MASCULINO, LocalDate.of(1990, 1, 1), LocalDate.of(2025, 1, 1),
                StatusAtendimento.RASCUNHO, outro);

        mockMvc.perform(patch("/api/atendimentos/{id}/antropometria", atendimento.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pesoKg\":80.00,\"alturaCm\":180.0}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));
    }

    @Test
    @DisplayName("supervisor não escreve na antropometria: 403")
    void supervisorNaoEscreve() throws Exception {
        Atendimento atendimento = novoAtendimento(
                Sexo.MASCULINO, LocalDate.of(1990, 1, 1), LocalDate.of(2025, 1, 1), StatusAtendimento.RASCUNHO);

        mockMvc.perform(patch("/api/atendimentos/{id}/antropometria", atendimento.getId())
                        .header("Authorization", "Bearer " + token(SUPERVISOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pesoKg\":80.00,\"alturaCm\":180.0}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));
    }

    @Test
    @DisplayName("atendimento inexistente devolve 404")
    void atendimentoInexistente() throws Exception {
        mockMvc.perform(patch("/api/atendimentos/{id}/antropometria", 999_999)
                        .header("Authorization", "Bearer " + token(ESTAGIARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pesoKg\":80.00,\"alturaCm\":180.0}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("sem token não passa da cadeia de filtros: 401")
    void semToken() throws Exception {
        mockMvc.perform(patch("/api/atendimentos/{id}/antropometria", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pesoKg\":80.00,\"alturaCm\":180.0}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTORIZADO"));
    }

    @Test
    @DisplayName("peso fora da faixa do banco vira 422, não erro de constraint")
    void pesoForaDaFaixa() throws Exception {
        Atendimento atendimento = novoAtendimento(
                Sexo.MASCULINO, LocalDate.of(1990, 1, 1), LocalDate.of(2025, 1, 1), StatusAtendimento.RASCUNHO);

        mockMvc.perform(patch("/api/atendimentos/{id}/antropometria", atendimento.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pesoKg\":500.00,\"alturaCm\":180.0}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.campos[0].campo").value("pesoKg"));
    }

    @Test
    @DisplayName("salvar de novo substitui a seção: medida removida zera o indicador")
    void salvarNovamenteSubstituiASecao() throws Exception {
        Atendimento atendimento = novoAtendimento(
                Sexo.MASCULINO, LocalDate.of(1990, 1, 1), LocalDate.of(2025, 1, 1), StatusAtendimento.RASCUNHO);

        mockMvc.perform(patch("/api/atendimentos/{id}/antropometria", atendimento.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pesoKg\":80.00,\"alturaCm\":180.0,\"circCinturaCm\":95.0,\"circQuadrilCm\":100.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relacaoCinturaQuadril").value(0.95));

        // Segundo salvamento sem as circunferências: o corpo é a seção inteira.
        mockMvc.perform(patch("/api/atendimentos/{id}/antropometria", atendimento.getId())
                        .header("Authorization", "Bearer " + token(ESTAGIARIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pesoKg\":80.00,\"alturaCm\":180.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imc").value(24.69))
                .andExpect(jsonPath("$.relacaoCinturaQuadril").doesNotExist())
                .andExpect(jsonPath("$.rcqElevada").doesNotExist())
                .andExpect(jsonPath("$.riscoCardiovascular").doesNotExist());
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private Atendimento novoAtendimento(Sexo sexo,
                                        LocalDate dataNascimento,
                                        LocalDate dataConsulta,
                                        StatusAtendimento status) {
        return novoAtendimento(sexo, dataNascimento, dataConsulta, status,
                usuarioPorEmail(ESTAGIARIO));
    }

    private Atendimento novoAtendimento(Sexo sexo,
                                        LocalDate dataNascimento,
                                        LocalDate dataConsulta,
                                        StatusAtendimento status,
                                        Usuario estagiario) {
        Usuario supervisor = usuarioPorEmail(SUPERVISOR);

        Paciente paciente = new Paciente();
        paciente.setNome("Paciente de Teste");
        paciente.setDataNascimento(dataNascimento);
        paciente.setSexo(sexo);
        paciente.setCriadoPor(estagiario);
        paciente = pacienteRepository.save(paciente);

        Atendimento atendimento = new Atendimento();
        atendimento.setNumeroProntuario("IT-" + SEQUENCIA.incrementAndGet() + "-" + System.nanoTime());
        atendimento.setPaciente(paciente);
        atendimento.setEstagiario(estagiario);
        atendimento.setSupervisor(supervisor);
        atendimento.setDataConsulta(dataConsulta);
        atendimento.setStatus(status);
        return atendimentoRepository.save(atendimento);
    }

    private Usuario novoEstagiario() {
        Usuario usuario = new Usuario();
        usuario.setNome("Outro Estagiário");
        usuario.setEmail("outro-" + SEQUENCIA.incrementAndGet() + "@nutriclinica.edu.br");
        // Nunca autentica neste teste; o hash só precisa satisfazer o NOT NULL.
        usuario.setSenhaHash("$2a$10$ppghIu4XhjdMoMfpR.n5rOaOVhcgTyADMIjM3Yiymp9utFNtx8fM6");
        usuario.setPerfil(Perfil.ESTAGIARIO);
        usuario.setAtivo(true);
        return usuarioRepository.save(usuario);
    }

    private Usuario usuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email).orElseThrow();
    }

    private String token(String email) throws Exception {
        String corpo = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode resposta = objectMapper.readTree(corpo);
        return resposta.get("accessToken").asText();
    }
}
