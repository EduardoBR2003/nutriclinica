package br.edu.nutriclinica.service;

import br.edu.nutriclinica.domain.Paciente;
import br.edu.nutriclinica.domain.TermoConsentimento;
import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.domain.enums.StatusAtendimento;
import br.edu.nutriclinica.dto.PacienteRequest;
import br.edu.nutriclinica.dto.PacienteResponse;
import br.edu.nutriclinica.dto.PaginaResponse;
import br.edu.nutriclinica.dto.PontoEvolucaoResponse;
import br.edu.nutriclinica.dto.TermoConsentimentoRequest;
import br.edu.nutriclinica.dto.TermoConsentimentoResponse;
import br.edu.nutriclinica.exception.NaoEncontradoException;
import br.edu.nutriclinica.repository.AntropometriaRepository;
import br.edu.nutriclinica.repository.PacienteRepository;
import br.edu.nutriclinica.repository.TermoConsentimentoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Cadastro de pacientes, termo de consentimento e série de evolução.
 *
 * <p>Toda leitura passa pelo escopo de LGPD, que é predicado da query no
 * repositório e não filtro em Java. Paciente fora do escopo devolve <b>404</b>,
 * nunca 403: um 403 confirmaria que aquele id existe, e a existência de um
 * prontuário alheio já é informação sensível.
 */
@Service
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final TermoConsentimentoRepository termoConsentimentoRepository;
    private final AntropometriaRepository antropometriaRepository;
    private final AuthService authService;

    public PacienteService(PacienteRepository pacienteRepository,
                           TermoConsentimentoRepository termoConsentimentoRepository,
                           AntropometriaRepository antropometriaRepository,
                           AuthService authService) {
        this.pacienteRepository = pacienteRepository;
        this.termoConsentimentoRepository = termoConsentimentoRepository;
        this.antropometriaRepository = antropometriaRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<PacienteResponse> listar(String busca, Pageable pageable) {
        Usuario usuario = authService.usuarioLogado();
        // Busca ausente vira string vazia, que casa com qualquer nome: evita um
        // `(:busca is null or ...)` e, com ele, parâmetro sem tipo no Postgres.
        String termoBusca = busca == null ? "" : busca.trim();

        Page<Paciente> pagina = switch (usuario.getPerfil()) {
            case ESTAGIARIO -> pacienteRepository.listarParaEstagiario(usuario.getId(), termoBusca, pageable);
            case SUPERVISOR -> pacienteRepository.listarParaSupervisor(usuario.getId(), termoBusca, pageable);
            case ADMIN -> pacienteRepository.listarParaAdmin(termoBusca, pageable);
        };

        Set<Long> comTermo = idsComTermo(pagina.getContent().stream().map(Paciente::getId).toList());
        List<PacienteResponse> conteudo = pagina.getContent().stream()
                .map(paciente -> PacienteResponse.de(paciente, comTermo.contains(paciente.getId())))
                .toList();

        return PaginaResponse.de(pagina, conteudo);
    }

    @Transactional(readOnly = true)
    public PacienteResponse detalhar(Long id) {
        Paciente paciente = carregarVisivel(id, authService.usuarioLogado());
        return PacienteResponse.de(paciente, possuiTermo(paciente.getId()));
    }

    /**
     * Cadastra o paciente em nome de quem está autenticado. É o {@code criadoPor}
     * que o coloca no escopo do estagiário antes mesmo de existir atendimento.
     */
    @Transactional
    public PacienteResponse criar(PacienteRequest requisicao) {
        Paciente paciente = new Paciente();
        aplicar(requisicao, paciente);
        paciente.setCriadoPor(authService.usuarioLogado());

        // Paciente recém-cadastrado ainda não tem termo — daí o `false` fixo.
        return PacienteResponse.de(pacienteRepository.save(paciente), false);
    }

    @Transactional
    public PacienteResponse atualizar(Long id, PacienteRequest requisicao) {
        Paciente paciente = carregarVisivel(id, authService.usuarioLogado());
        aplicar(requisicao, paciente);

        return PacienteResponse.de(pacienteRepository.save(paciente), possuiTermo(paciente.getId()));
    }

    @Transactional(readOnly = true)
    public TermoConsentimentoResponse buscarTermo(Long pacienteId) {
        Paciente paciente = carregarVisivel(pacienteId, authService.usuarioLogado());

        return termoConsentimentoRepository.findByPacienteId(paciente.getId())
                .map(TermoConsentimentoResponse::de)
                .orElseThrow(() -> new NaoEncontradoException(
                        "O paciente " + pacienteId + " ainda não tem termo de consentimento registrado."));
    }

    /**
     * Registra ou atualiza o termo. Há no máximo um por paciente (UNIQUE em
     * {@code paciente_id}), então o PUT do contrato é um upsert.
     *
     * <p>{@code registradoPor} é sempre o usuário autenticado: quem registrou o
     * consentimento é dado de auditoria de LGPD, não algo que o cliente escolhe.
     */
    @Transactional
    public TermoConsentimentoResponse salvarTermo(Long pacienteId, TermoConsentimentoRequest requisicao) {
        Usuario usuario = authService.usuarioLogado();
        Paciente paciente = carregarVisivel(pacienteId, usuario);

        TermoConsentimento termo = termoConsentimentoRepository.findByPacienteId(paciente.getId())
                .orElseGet(() -> {
                    TermoConsentimento novo = new TermoConsentimento();
                    novo.setPaciente(paciente);
                    return novo;
                });

        termo.setAceiteLgpd(requisicao.aceiteLgpd());
        termo.setAutorizaUsoPesquisa(requisicao.autorizaUsoPesquisaOuFalso());
        termo.setDataAceite(requisicao.dataAceite());
        termo.setObservacoes(requisicao.observacoes());
        termo.setRegistradoPor(usuario);

        return TermoConsentimentoResponse.de(termoConsentimentoRepository.save(termo));
    }

    /**
     * Série histórica para o gráfico de evolução, montada com as antropometrias
     * dos atendimentos <b>aprovados</b> do paciente, em ordem de data de consulta.
     *
     * <p>O escopo é conferido no paciente. Depois disso a série traz todos os
     * atendimentos aprovados dele, inclusive os de outro estagiário: a evolução
     * de peso só faz sentido clínico como trajetória inteira, e quem chegou até
     * aqui já tem o paciente no seu escopo.
     */
    @Transactional(readOnly = true)
    public List<PontoEvolucaoResponse> evolucao(Long pacienteId) {
        Paciente paciente = carregarVisivel(pacienteId, authService.usuarioLogado());
        return antropometriaRepository.serieDoPaciente(paciente.getId(), StatusAtendimento.APROVADO);
    }

    /**
     * Carrega o paciente aplicando o escopo do usuário. Fora do escopo é
     * indistinguível de inexistente — de propósito.
     */
    @Transactional(readOnly = true)
    public Paciente carregarVisivel(Long id, Usuario usuario) {
        Optional<Paciente> visivel = switch (usuario.getPerfil()) {
            case ESTAGIARIO -> pacienteRepository.buscarVisivelPeloEstagiario(id, usuario.getId());
            case SUPERVISOR -> pacienteRepository.buscarVisivelPeloSupervisor(id, usuario.getId());
            case ADMIN -> pacienteRepository.findById(id);
        };

        return visivel.orElseThrow(() -> NaoEncontradoException.de("Paciente", id));
    }

    private void aplicar(PacienteRequest requisicao, Paciente paciente) {
        paciente.setNome(requisicao.nome().trim());
        paciente.setDataNascimento(requisicao.dataNascimento());
        paciente.setSexo(requisicao.sexo());
        paciente.setRacaCor(requisicao.racaCor());
        paciente.setTelefone(requisicao.telefone());
        paciente.setEmail(requisicao.email());
    }

    private boolean possuiTermo(Long pacienteId) {
        return termoConsentimentoRepository.existsByPacienteIdAndAceiteLgpdTrue(pacienteId);
    }

    private Set<Long> idsComTermo(List<Long> pacienteIds) {
        if (pacienteIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(termoConsentimentoRepository.idsComAceite(pacienteIds));
    }
}
