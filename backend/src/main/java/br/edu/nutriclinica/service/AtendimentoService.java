package br.edu.nutriclinica.service;

import br.edu.nutriclinica.domain.Atendimento;
import br.edu.nutriclinica.domain.Paciente;
import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.domain.enums.Perfil;
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
import br.edu.nutriclinica.service.auditoria.Auditavel;
import br.edu.nutriclinica.service.secao.ProntuarioCompletoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Abertura e leitura de atendimentos.
 *
 * <p>Como em {@link PacienteService}, o escopo de LGPD é predicado da query e
 * atendimento fora dele devolve 404.
 *
 * <p>Aqui o atendimento só <b>nasce</b>, sempre em RASCUNHO. Mudá-lo de estado é
 * do {@link AtendimentoWorkflowService}, que é o único ponto do sistema com essa
 * permissão — nem este serviço escreve {@code status}.
 */
@Service
public class AtendimentoService {

    private final AtendimentoRepository atendimentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final VinculoSupervisaoRepository vinculoSupervisaoRepository;
    private final TermoConsentimentoRepository termoConsentimentoRepository;
    private final PacienteService pacienteService;
    private final NumeroProntuarioService numeroProntuarioService;
    private final AtendimentoEditavelValidator atendimentoEditavelValidator;
    private final ProntuarioCompletoService prontuarioCompletoService;
    private final AvaliacaoService avaliacaoService;
    private final AtendimentoResponseAssembler assembler;
    private final AuthService authService;

    public AtendimentoService(AtendimentoRepository atendimentoRepository,
                              UsuarioRepository usuarioRepository,
                              VinculoSupervisaoRepository vinculoSupervisaoRepository,
                              TermoConsentimentoRepository termoConsentimentoRepository,
                              PacienteService pacienteService,
                              NumeroProntuarioService numeroProntuarioService,
                              AtendimentoEditavelValidator atendimentoEditavelValidator,
                              ProntuarioCompletoService prontuarioCompletoService,
                              AvaliacaoService avaliacaoService,
                              AtendimentoResponseAssembler assembler,
                              AuthService authService) {
        this.atendimentoRepository = atendimentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.vinculoSupervisaoRepository = vinculoSupervisaoRepository;
        this.termoConsentimentoRepository = termoConsentimentoRepository;
        this.pacienteService = pacienteService;
        this.numeroProntuarioService = numeroProntuarioService;
        this.atendimentoEditavelValidator = atendimentoEditavelValidator;
        this.prontuarioCompletoService = prontuarioCompletoService;
        this.avaliacaoService = avaliacaoService;
        this.assembler = assembler;
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

        return PaginaResponse.de(pagina, assembler.emLote(pagina.getContent(), usuario));
    }

    /**
     * Prontuário do atendimento: os campos base, as onze seções e a avaliação.
     *
     * <p>O escopo é aplicado uma vez, aqui, e as seções são lidas a partir do
     * atendimento já carregado — nenhuma delas repete a consulta de acesso.
     *
     * <p>É a leitura auditada do bloco: abrir um prontuário é acesso a dado
     * sensível de saúde, e a LGPD quer saber quem o fez.
     */
    @Auditavel(acao = "LEITURA_PRONTUARIO", entidade = "Atendimento")
    @Transactional(readOnly = true)
    public AtendimentoCompletoResponse detalhar(Long id) {
        Usuario usuario = authService.usuarioLogado();
        Atendimento atendimento = atendimentoEditavelValidator.carregarParaLeitura(id, usuario);

        return AtendimentoCompletoResponse.de(
                assembler.unico(atendimento, usuario),
                prontuarioCompletoService.secoesDe(atendimento),
                avaliacaoService.doAtendimento(atendimento).orElse(null));
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
        // O status não é atribuído aqui: RASCUNHO é o estado de nascimento,
        // declarado na própria entidade. Escrever status é privilégio exclusivo
        // do AtendimentoWorkflowService, e abrir um atendimento não é transição.

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

}
