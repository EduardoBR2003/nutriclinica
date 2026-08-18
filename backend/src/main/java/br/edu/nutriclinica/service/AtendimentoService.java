package br.edu.nutriclinica.service;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.Paciente;
import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.domain.enums.Perfil;
import br.edu.nutriclinica.domain.enums.SecaoProntuario;
import br.edu.nutriclinica.domain.enums.StatusAtendimento;
import br.edu.nutriclinica.dto.AtendimentoCompletoResponse;
import br.edu.nutriclinica.dto.AtendimentoRequest;
import br.edu.nutriclinica.dto.AtendimentoResponse;
import br.edu.nutriclinica.dto.PaginaResponse;
import br.edu.nutriclinica.exception.SemTermoConsentimentoException;
import br.edu.nutriclinica.exception.ValidacaoException;
import br.edu.nutriclinica.repository.AtendimentoRepository;
import br.edu.nutriclinica.repository.TermoConsentimentoRepository;
import br.edu.nutriclinica.repository.UsuarioRepository;
import br.edu.nutriclinica.repository.VinculoSupervisaoRepository;
import br.edu.nutriclinica.service.secao.ProntuarioCompletoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abertura e leitura de atendimentos.
 *
 * <p>Como em {@link PacienteService}, o escopo de LGPD é predicado da query e
 * atendimento fora dele devolve 404. A máquina de estados propriamente dita
 * (submeter, aprovar, devolver) é do bloco seguinte; aqui o atendimento só
 * nasce em RASCUNHO.
 */
@Service
public class AtendimentoService {

    private final AtendimentoRepository atendimentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final VinculoSupervisaoRepository vinculoSupervisaoRepository;
    private final TermoConsentimentoRepository termoConsentimentoRepository;
    private final PacienteService pacienteService;
    private final NumeroProntuarioService numeroProntuarioService;
    private final SecoesPreenchidasService secoesPreenchidasService;
    private final AtendimentoEditavelValidator atendimentoEditavelValidator;
    private final ProntuarioCompletoService prontuarioCompletoService;
    private final AuthService authService;

    public AtendimentoService(AtendimentoRepository atendimentoRepository,
                              UsuarioRepository usuarioRepository,
                              VinculoSupervisaoRepository vinculoSupervisaoRepository,
                              TermoConsentimentoRepository termoConsentimentoRepository,
                              PacienteService pacienteService,
                              NumeroProntuarioService numeroProntuarioService,
                              SecoesPreenchidasService secoesPreenchidasService,
                              AtendimentoEditavelValidator atendimentoEditavelValidator,
                              ProntuarioCompletoService prontuarioCompletoService,
                              AuthService authService) {
        this.atendimentoRepository = atendimentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.vinculoSupervisaoRepository = vinculoSupervisaoRepository;
        this.termoConsentimentoRepository = termoConsentimentoRepository;
        this.pacienteService = pacienteService;
        this.numeroProntuarioService = numeroProntuarioService;
        this.secoesPreenchidasService = secoesPreenchidasService;
        this.atendimentoEditavelValidator = atendimentoEditavelValidator;
        this.prontuarioCompletoService = prontuarioCompletoService;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<AtendimentoResponse> listar(StatusAtendimento status,
                                                      Long pacienteId,
                                                      Pageable pageable) {
        Usuario usuario = authService.usuarioLogado();

        Page<Atendimento> pagina = switch (usuario.getPerfil()) {
            case ESTAGIARIO -> atendimentoRepository.listarParaEstagiario(usuario.getId(), status, pacienteId, pageable);
            case SUPERVISOR -> atendimentoRepository.listarParaSupervisor(usuario.getId(), status, pacienteId, pageable);
            case ADMIN -> atendimentoRepository.listarParaAdmin(status, pacienteId, pageable);
        };

        return PaginaResponse.de(pagina, converterEmLote(pagina.getContent(), usuario));
    }

    /**
     * Prontuário do atendimento: os campos base mais as onze seções.
     *
     * <p>O escopo é aplicado uma vez, aqui, e as seções são lidas a partir do
     * atendimento já carregado — nenhuma delas repete a consulta de acesso.
     */
    @Transactional(readOnly = true)
    public AtendimentoCompletoResponse detalhar(Long id) {
        Usuario usuario = authService.usuarioLogado();
        Atendimento atendimento = atendimentoEditavelValidator.carregarParaLeitura(id, usuario);

        AtendimentoResponse base = AtendimentoResponse.de(
                atendimento,
                possuiTermo(atendimento.getPaciente().getId()),
                atendimentoEditavelValidator.editavelPor(atendimento, usuario),
                secoesPreenchidasService.doAtendimento(atendimento.getId()));

        return AtendimentoCompletoResponse.de(base, prontuarioCompletoService.secoesDe(atendimento));
    }

    /**
     * Abre um atendimento em RASCUNHO.
     *
     * <p>A ordem das validações é deliberada: o escopo vem primeiro, para que um
     * paciente que o estagiário não pode ver responda 404 sem antes revelar, por
     * um 409, se ele tem ou não termo registrado.
     */
    @Transactional
    public AtendimentoResponse criar(AtendimentoRequest requisicao) {
        Usuario estagiario = authService.usuarioLogado();

        Paciente paciente = pacienteService.carregarVisivel(requisicao.pacienteId(), estagiario);

        if (!termoConsentimentoRepository.existsByPacienteIdAndAceiteLgpdTrue(paciente.getId())) {
            throw new SemTermoConsentimentoException(paciente.getId());
        }

        Usuario supervisor = validarSupervisor(requisicao.supervisorId(), estagiario);

        Atendimento atendimento = new Atendimento();
        atendimento.setNumeroProntuario(numeroProntuarioService.gerar(requisicao.dataConsulta()));
        atendimento.setPaciente(paciente);
        atendimento.setEstagiario(estagiario);
        atendimento.setSupervisor(supervisor);
        atendimento.setDataConsulta(requisicao.dataConsulta());
        atendimento.setStatus(StatusAtendimento.RASCUNHO);

        atendimento = atendimentoRepository.save(atendimento);

        // Recém-criado: nenhuma seção preenchida, e editável por ser RASCUNHO do
        // próprio estagiário.
        return AtendimentoResponse.de(atendimento, true, true, Set.of());
    }

    /**
     * O supervisor precisa existir, ter o perfil SUPERVISOR e orientar quem está
     * abrindo o atendimento. Sem o vínculo, um estagiário poderia despachar o
     * próprio prontuário para a fila de revisão de qualquer professor da casa.
     */
    private Usuario validarSupervisor(Long supervisorId, Usuario estagiario) {
        Usuario supervisor = usuarioRepository.findById(supervisorId)
                .orElseThrow(() -> new ValidacaoException("supervisorId",
                        "Supervisor " + supervisorId + " não encontrado."));

        if (supervisor.getPerfil() != Perfil.SUPERVISOR) {
            throw new ValidacaoException("supervisorId",
                    "O usuário informado não tem perfil SUPERVISOR.");
        }

        if (!vinculoSupervisaoRepository.existsBySupervisorIdAndEstagiarioIdAndAtivoTrue(
                supervisor.getId(), estagiario.getId())) {
            throw new ValidacaoException("supervisorId",
                    "O supervisor informado não orienta você. Peça ao administrador para criar o vínculo.");
        }
        return supervisor;
    }

    /**
     * Converte a página inteira com duas consultas de apoio — termos e seções —
     * em vez de duas por atendimento.
     */
    private List<AtendimentoResponse> converterEmLote(List<Atendimento> atendimentos, Usuario usuario) {
        if (atendimentos.isEmpty()) {
            return List.of();
        }

        List<Long> atendimentoIds = atendimentos.stream().map(Atendimento::getId).toList();
        List<Long> pacienteIds = atendimentos.stream().map(a -> a.getPaciente().getId()).distinct().toList();

        Set<Long> comTermo = Set.copyOf(termoConsentimentoRepository.idsComAceite(pacienteIds));
        Map<Long, Set<SecaoProntuario>> secoes = secoesPreenchidasService.porAtendimento(atendimentoIds);

        return atendimentos.stream()
                .map(atendimento -> AtendimentoResponse.de(
                        atendimento,
                        comTermo.contains(atendimento.getPaciente().getId()),
                        atendimentoEditavelValidator.editavelPor(atendimento, usuario),
                        secoes.getOrDefault(atendimento.getId(), Set.of())))
                .toList();
    }

    private boolean possuiTermo(Long pacienteId) {
        return termoConsentimentoRepository.existsByPacienteIdAndAceiteLgpdTrue(pacienteId);
    }
}
