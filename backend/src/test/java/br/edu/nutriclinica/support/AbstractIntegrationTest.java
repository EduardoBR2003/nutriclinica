package br.edu.nutriclinica.support;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.Paciente;
import br.edu.nutriclinica.domain.TermoConsentimento;
import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.domain.VinculoSupervisao;
import br.edu.nutriclinica.domain.enums.Perfil;
import br.edu.nutriclinica.domain.enums.Sexo;
import br.edu.nutriclinica.domain.enums.StatusAtendimento;
import br.edu.nutriclinica.repository.AtendimentoRepository;
import br.edu.nutriclinica.repository.PacienteRepository;
import br.edu.nutriclinica.repository.TermoConsentimentoRepository;
import br.edu.nutriclinica.repository.UsuarioRepository;
import br.edu.nutriclinica.repository.VinculoSupervisaoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes de integração: PostgreSQL 17 real via Testcontainers, migrations
 * do Flyway aplicadas (inclusive o seed do V2) e MockMvc passando pela cadeia de
 * filtros do Spring Security.
 *
 * O container é estático e não é fechado de propósito: o Testcontainers o reaproveita
 * entre as classes de teste e o encerra ao fim da JVM (Ryuk).
 *
 * O perfil "it" está na lista do SeedDesenvolvimentoRunner, e é dele que vêm os
 * três usuários abaixo — o seed saiu do Flyway para não alcançar produção.
 *
 * As fixtures abaixo montam os dados direto pelos repositories, sem passar pela
 * API: o que cada teste quer exercitar é o endpoint sob teste, não a construção
 * do cenário. O banco é compartilhado entre as classes, então nenhum teste deve
 * depender de contagem total de registros.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
public abstract class AbstractIntegrationTest {

    /** Usuários do SeedDesenvolvimentoRunner. O supervisor já orienta o estagiário. */
    protected static final String ESTAGIARIO = "estagiario@nutriclinica.edu.br";
    protected static final String SUPERVISOR = "supervisor@nutriclinica.edu.br";
    protected static final String ADMIN = "admin@nutriclinica.edu.br";
    protected static final String SENHA = "senha123";

    /** Hash BCrypt de "senha123", a mesma senha do seed: usuários criados aqui também logam. */
    protected static final String HASH_SENHA123 = "$2a$10$ppghIu4XhjdMoMfpR.n5rOaOVhcgTyADMIjM3Yiymp9utFNtx8fM6";

    /** Mantém e-mails e números de prontuário únicos sem depender da ordem dos testes. */
    private static final AtomicInteger SEQUENCIA = new AtomicInteger();

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UsuarioRepository usuarioRepository;
    @Autowired
    protected PacienteRepository pacienteRepository;
    @Autowired
    protected AtendimentoRepository atendimentoRepository;
    @Autowired
    protected TermoConsentimentoRepository termoConsentimentoRepository;
    @Autowired
    protected VinculoSupervisaoRepository vinculoSupervisaoRepository;

    // ------------------------------------------------------------------
    // Autenticação
    // ------------------------------------------------------------------

    /** Access token de verdade, obtido pelo /api/auth/login. */
    protected String token(String email) throws Exception {
        String corpo = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode resposta = objectMapper.readTree(corpo);
        return resposta.get("accessToken").asText();
    }

    protected Usuario usuarioPorEmail(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email).orElseThrow();
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    protected Usuario novoUsuario(String nome, Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail("teste-" + SEQUENCIA.incrementAndGet() + "@nutriclinica.edu.br");
        usuario.setSenhaHash(HASH_SENHA123);
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuarioRepository.save(usuario);
    }

    protected VinculoSupervisao vincular(Usuario supervisor, Usuario estagiario) {
        VinculoSupervisao vinculo = new VinculoSupervisao();
        vinculo.setSupervisor(supervisor);
        vinculo.setEstagiario(estagiario);
        vinculo.setAtivo(true);
        return vinculoSupervisaoRepository.save(vinculo);
    }

    protected Paciente novoPaciente(Usuario criadoPor) {
        return novoPaciente("Paciente de Teste " + SEQUENCIA.incrementAndGet(),
                Sexo.FEMININO, LocalDate.of(1990, 1, 1), criadoPor);
    }

    protected Paciente novoPaciente(String nome, Sexo sexo, LocalDate dataNascimento, Usuario criadoPor) {
        Paciente paciente = new Paciente();
        paciente.setNome(nome);
        paciente.setSexo(sexo);
        paciente.setDataNascimento(dataNascimento);
        paciente.setCriadoPor(criadoPor);
        return pacienteRepository.save(paciente);
    }

    protected TermoConsentimento registrarTermo(Paciente paciente, Usuario registradoPor, boolean aceite) {
        TermoConsentimento termo = new TermoConsentimento();
        termo.setPaciente(paciente);
        termo.setAceiteLgpd(aceite);
        termo.setAutorizaUsoPesquisa(false);
        termo.setDataAceite(LocalDate.now());
        termo.setRegistradoPor(registradoPor);
        return termoConsentimentoRepository.save(termo);
    }

    /**
     * Atendimento montado direto no banco, com número de prontuário sintético —
     * a numeração de verdade é exercitada pelo POST, no teste próprio dela.
     */
    protected Atendimento novoAtendimento(Paciente paciente,
                                          Usuario estagiario,
                                          Usuario supervisor,
                                          LocalDate dataConsulta,
                                          StatusAtendimento status) {
        Atendimento atendimento = new Atendimento();
        atendimento.setNumeroProntuario("IT-" + SEQUENCIA.incrementAndGet() + "-" + System.nanoTime());
        atendimento.setPaciente(paciente);
        atendimento.setEstagiario(estagiario);
        atendimento.setSupervisor(supervisor);
        atendimento.setDataConsulta(dataConsulta);
        atendimento.aplicarTransicao(status, LocalDateTime.now());
        return atendimentoRepository.save(atendimento);
    }

    /** O mesmo, com o instante da transição escolhido — a fila ordena por ele. */
    protected Atendimento novoAtendimento(Paciente paciente,
                                          Usuario estagiario,
                                          Usuario supervisor,
                                          LocalDate dataConsulta,
                                          StatusAtendimento status,
                                          LocalDateTime momento) {
        Atendimento atendimento = novoAtendimento(paciente, estagiario, supervisor, dataConsulta, status);
        atendimento.aplicarTransicao(status, momento);
        return atendimentoRepository.save(atendimento);
    }

    // ------------------------------------------------------------------
    // Prontuário pronto para submeter
    // ------------------------------------------------------------------

    /**
     * Preenche as cinco seções que a submissão exige, <b>pela API</b>.
     *
     * <p>Aqui a fixture não pode atalhar pelos repositories, ao contrário das de
     * cima. O portão de submissão existe justamente porque um PATCH com campos
     * nulos <i>cria a linha</i> da seção; um cenário escrito direto no banco
     * nunca reproduziria essa armadilha, e o teste passaria a validar algo que o
     * usuário não vive.
     *
     * @param token o do estagiário <b>dono</b> — os PATCH passam por exigirEditavel
     */
    protected void preencherSecoesObrigatorias(Long atendimentoId, String token) throws Exception {
        preencherQueixa(atendimentoId, token);
        preencherAntropometria(atendimentoId, token);
        preencherRecordatorio(atendimentoId, token);
        preencherDiagnostico(atendimentoId, token);
        preencherPlano(atendimentoId, token);
    }

    protected void preencherQueixa(Long atendimentoId, String token) throws Exception {
        secao("PATCH", atendimentoId, "queixa-principal", token,
                """
                {"motivo": "Ganho de peso nos últimos seis meses"}
                """);
    }

    protected void preencherAntropometria(Long atendimentoId, String token) throws Exception {
        secao("PATCH", atendimentoId, "antropometria", token,
                """
                {"pesoKg": 72.00, "alturaCm": 170.0}
                """);
    }

    protected void preencherRecordatorio(Long atendimentoId, String token) throws Exception {
        secao("PUT", atendimentoId, "recordatorio", token,
                """
                [{"tipoRefeicao": "ALMOCO", "horario": "12:00",
                  "itens": [{"alimento": "Arroz integral", "quantidade": "4 colheres"}]}]
                """);
    }

    protected void preencherDiagnostico(Long atendimentoId, String token) throws Exception {
        secao("PATCH", atendimentoId, "diagnostico", token,
                """
                {"problema": "Ingestão energética excessiva",
                 "etiologia": "Consumo frequente de ultraprocessados",
                 "sinaisSintomas": "IMC 24,9 e ganho de 6 kg em seis meses"}
                """);
    }

    protected void preencherPlano(Long atendimentoId, String token) throws Exception {
        secao("PATCH", atendimentoId, "plano", token,
                """
                {"objetivos": "Reduzir 4 kg em três meses com reeducação alimentar"}
                """);
    }

    /** Preenche uma seção e exige 200: a falha precisa ser atribuída à fixture, não ao caso. */
    private void secao(String metodo, Long atendimentoId, String secao, String token, String corpo)
            throws Exception {
        String url = "/api/atendimentos/" + atendimentoId + "/" + secao;

        mockMvc.perform(("PUT".equals(metodo) ? put(url) : patch(url))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk());
    }

    /** Submete o atendimento; o teste decide o que esperar da resposta. */
    protected ResultActions submeter(Long atendimentoId, String token) throws Exception {
        return mockMvc.perform(post("/api/atendimentos/{id}/submeter", atendimentoId)
                .header("Authorization", "Bearer " + token));
    }
}
