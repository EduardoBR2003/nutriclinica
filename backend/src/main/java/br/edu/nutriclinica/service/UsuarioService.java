package br.edu.nutriclinica.service;

import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.domain.VinculoSupervisao;
import br.edu.nutriclinica.domain.enums.Perfil;
import br.edu.nutriclinica.dto.CadastroRequest;
import br.edu.nutriclinica.dto.PaginaResponse;
import br.edu.nutriclinica.dto.UsuarioRequest;
import br.edu.nutriclinica.dto.UsuarioResponse;
import br.edu.nutriclinica.exception.NaoEncontradoException;
import br.edu.nutriclinica.exception.ValidacaoException;
import br.edu.nutriclinica.repository.UsuarioRepository;
import br.edu.nutriclinica.repository.VinculoSupervisaoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cadastro de usuários e vínculos de supervisão.
 *
 * <p>Duas portas levam a um usuário novo, e elas não são a mesma coisa:
 * {@link #criar} é o cadastro manual do ADMIN, que confia em quem cadastra e por
 * isso aceita qualquer perfil e já nasce ativo; {@link #autoCadastrar} é a tela
 * pública, que não confia em ninguém — perfil restrito a ESTAGIARIO/SUPERVISOR e
 * conta sempre inativa, à espera da ativação por um ADMIN. Sem essa separação a
 * tela pública seria uma fábrica de administradores.
 *
 * <p>A senha entra em texto claro e sai daqui só como hash BCrypt: nenhum outro
 * ponto do sistema escreve {@code senhaHash}.
 */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final VinculoSupervisaoRepository vinculoSupervisaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          VinculoSupervisaoRepository vinculoSupervisaoRepository,
                          PasswordEncoder passwordEncoder,
                          AuthService authService) {
        this.usuarioRepository = usuarioRepository;
        this.vinculoSupervisaoRepository = vinculoSupervisaoRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<UsuarioResponse> listar(Perfil perfil, Pageable pageable) {
        Page<Usuario> pagina = perfil == null
                ? usuarioRepository.findAll(pageable)
                : usuarioRepository.findByPerfil(perfil, pageable);

        return PaginaResponse.de(pagina, pagina.getContent().stream()
                .map(UsuarioResponse::de)
                .toList());
    }

    @Transactional(readOnly = true)
    public UsuarioResponse detalhar(Long id) {
        return UsuarioResponse.de(carregar(id));
    }

    /** Cadastro manual pelo ADMIN. */
    @Transactional
    public UsuarioResponse criar(UsuarioRequest requisicao) {
        if (!requisicao.trocaSenha()) {
            throw new ValidacaoException("senha", "A senha é obrigatória no cadastro.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(requisicao.nome().trim());
        usuario.setEmail(emailDisponivel(requisicao.email(), null));
        usuario.setPerfil(requisicao.perfil());
        usuario.setAtivo(requisicao.ativoOuVerdadeiro());
        usuario.setSenhaHash(passwordEncoder.encode(requisicao.senha()));

        return UsuarioResponse.de(usuarioRepository.save(usuario));
    }

    /**
     * Auto-cadastro pela tela de entrada: conta inativa, perfil restrito.
     *
     * <p>O 422 de e-mail repetido é o mesmo do cadastro do ADMIN. Isso de fato
     * revela que aquele e-mail já tem conta, mas a alternativa — aceitar em
     * silêncio — deixaria a pessoa esperando por uma ativação que nunca viria.
     */
    @Transactional
    public UsuarioResponse autoCadastrar(CadastroRequest requisicao) {
        if (requisicao.perfil() == Perfil.ADMIN) {
            throw new ValidacaoException("perfil",
                    "Conta de administrador só é criada por outro administrador.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(requisicao.nome().trim());
        usuario.setEmail(emailDisponivel(requisicao.email(), null));
        usuario.setPerfil(requisicao.perfil());
        // Inativa de propósito: quem se cadastra não se autoriza. O ADMIN ativa.
        usuario.setAtivo(false);
        usuario.setSenhaHash(passwordEncoder.encode(requisicao.senha()));

        return UsuarioResponse.de(usuarioRepository.save(usuario));
    }

    /**
     * Atualiza o usuário. Senha ausente ou em branco mantém a atual — a tela
     * nunca recebeu o hash e não teria como devolvê-lo.
     *
     * <p>Mudar o perfil desfaz os vínculos de supervisão da pessoa: um
     * ex-supervisor não pode continuar na lista de quem revisa, e um
     * ex-estagiário não pode continuar orientado. Os atendimentos já abertos
     * seguem intactos, apontando para o usuário como sempre apontaram.
     */
    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioRequest requisicao) {
        Usuario usuario = carregar(id);
        Perfil perfilAnterior = usuario.getPerfil();

        usuario.setNome(requisicao.nome().trim());
        usuario.setEmail(emailDisponivel(requisicao.email(), id));
        usuario.setPerfil(requisicao.perfil());
        usuario.setAtivo(requisicao.ativoOuVerdadeiro());

        if (requisicao.trocaSenha()) {
            usuario.setSenhaHash(passwordEncoder.encode(requisicao.senha()));
        }

        if (perfilAnterior != requisicao.perfil()) {
            desativarVinculosDe(usuario);
        }

        return UsuarioResponse.de(usuarioRepository.save(usuario));
    }

    /** Supervisores que o estagiário autenticado pode escolher ao abrir atendimento. */
    @Transactional(readOnly = true)
    public List<UsuarioResponse> supervisoresDisponiveis() {
        Usuario estagiario = authService.usuarioLogado();
        return vinculoSupervisaoRepository.supervisoresDoEstagiario(estagiario.getId()).stream()
                .map(UsuarioResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> estagiariosVinculados(Long supervisorId) {
        exigirSupervisor(carregar(supervisorId));
        return vinculoSupervisaoRepository.estagiariosDoSupervisor(supervisorId).stream()
                .map(UsuarioResponse::de)
                .toList();
    }

    /**
     * Substitui o conjunto de orientados de um supervisor.
     *
     * <p>Reconciliação, não recriação: a linha que já existe é reaproveitada e
     * só troca de {@code ativo}. Apagar e reinserir perderia o {@code criadoEm}
     * do vínculo e esbarraria no UNIQUE {@code (supervisor_id, estagiario_id)}
     * assim que alguém fosse removido e devolvido ao mesmo supervisor.
     */
    @Transactional
    public void definirVinculos(Long supervisorId, List<Long> estagiarioIds) {
        Usuario supervisor = exigirSupervisor(carregar(supervisorId));
        Set<Long> desejados = new HashSet<>(estagiarioIds);

        Map<Long, Usuario> estagiarios = carregarEstagiarios(desejados);

        List<VinculoSupervisao> existentes = vinculoSupervisaoRepository.findBySupervisorId(supervisorId);
        for (VinculoSupervisao vinculo : existentes) {
            Long estagiarioId = vinculo.getEstagiario().getId();
            vinculo.setAtivo(desejados.remove(estagiarioId));
        }
        vinculoSupervisaoRepository.saveAll(existentes);

        // O que sobrou em `desejados` ainda não tinha linha nenhuma.
        List<VinculoSupervisao> novos = desejados.stream()
                .map(estagiarioId -> {
                    VinculoSupervisao vinculo = new VinculoSupervisao();
                    vinculo.setSupervisor(supervisor);
                    vinculo.setEstagiario(estagiarios.get(estagiarioId));
                    vinculo.setAtivo(true);
                    return vinculo;
                })
                .toList();
        vinculoSupervisaoRepository.saveAll(novos);
    }

    private Usuario carregar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> NaoEncontradoException.de("Usuário", id));
    }

    private Usuario exigirSupervisor(Usuario usuario) {
        if (usuario.getPerfil() != Perfil.SUPERVISOR) {
            throw new ValidacaoException("supervisorId",
                    "Só um usuário com perfil SUPERVISOR tem estagiários vinculados.");
        }
        return usuario;
    }

    /**
     * Carrega os estagiários informados conferindo, de uma vez, que todos existem
     * e que todos são de fato ESTAGIARIO — vincular um supervisor a outro
     * supervisor produziria uma fila de revisão que ninguém consegue atender.
     */
    private Map<Long, Usuario> carregarEstagiarios(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<Long, Usuario> encontrados = usuarioRepository.findByIdIn(List.copyOf(ids)).stream()
                .collect(Collectors.toMap(Usuario::getId, Function.identity()));

        for (Long id : ids) {
            Usuario estagiario = encontrados.get(id);
            if (estagiario == null) {
                throw new ValidacaoException("estagiarioIds", "Usuário " + id + " não encontrado.");
            }
            if (estagiario.getPerfil() != Perfil.ESTAGIARIO) {
                throw new ValidacaoException("estagiarioIds",
                        "O usuário " + estagiario.getNome() + " não tem perfil ESTAGIARIO.");
            }
        }
        return encontrados;
    }

    private void desativarVinculosDe(Usuario usuario) {
        List<VinculoSupervisao> afetados = new ArrayList<>(
                vinculoSupervisaoRepository.findBySupervisorIdAndAtivoTrue(usuario.getId()));
        afetados.addAll(vinculoSupervisaoRepository.findByEstagiarioIdAndAtivoTrue(usuario.getId()));

        afetados.forEach(vinculo -> vinculo.setAtivo(false));
        vinculoSupervisaoRepository.saveAll(afetados);
    }

    /**
     * Normaliza o e-mail e garante que ele ainda não é de outra conta. A
     * comparação ignora a caixa porque {@code Ana@x.br} e {@code ana@x.br} são o
     * mesmo endereço para quem manda a mensagem — e seriam dois logins distintos
     * se o UNIQUE do banco fosse a única defesa.
     */
    private String emailDisponivel(String email, Long idAtual) {
        String normalizado = email.trim().toLowerCase();

        boolean emUso = idAtual == null
                ? usuarioRepository.existsByEmailIgnoreCase(normalizado)
                : usuarioRepository.existsByEmailIgnoreCaseAndIdNot(normalizado, idAtual);

        if (emUso) {
            throw new ValidacaoException("email", "Já existe um usuário com este e-mail.");
        }
        return normalizado;
    }
}
