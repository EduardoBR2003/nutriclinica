package br.edu.nutriclinica.config.seed;

import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.domain.VinculoSupervisao;
import br.edu.nutriclinica.domain.enums.Perfil;
import br.edu.nutriclinica.repository.UsuarioRepository;
import br.edu.nutriclinica.repository.VinculoSupervisaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Usuários de demonstração — um por perfil, com o vínculo entre supervisor e
 * estagiário —, só nos perfis dev e it.
 *
 * <p>Isto morava no {@code V2__seed.sql}, e era um problema: migration roda em
 * todo ambiente que o Flyway alcança, inclusive produção. Três contas com senha
 * conhecida e publicada no repositório entrariam no ar junto com o schema. O
 * perfil aqui é a fronteira que o Flyway não tem.
 *
 * <p>O perfil {@code it} entra na lista porque os testes de integração
 * autenticam com estes usuários e contam com o vínculo já criado. Deixá-los
 * montar o cenário por conta própria só transformaria o mesmo seed em código
 * duplicado no módulo de teste.
 *
 * <p>Idempotente por e-mail: rodar de novo sobre uma base já semeada não
 * duplica nada nem reescreve senha trocada à mão.
 */
@Component
@Profile({"dev", "it"})
public class SeedDesenvolvimentoRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDesenvolvimentoRunner.class);

    /** Conhecida e de propósito: é dado de demonstração, não credencial. */
    private static final String SENHA_DEMO = "senha123";

    private final UsuarioRepository usuarioRepository;
    private final VinculoSupervisaoRepository vinculoSupervisaoRepository;
    private final PasswordEncoder passwordEncoder;

    public SeedDesenvolvimentoRunner(UsuarioRepository usuarioRepository,
                                     VinculoSupervisaoRepository vinculoSupervisaoRepository,
                                     PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.vinculoSupervisaoRepository = vinculoSupervisaoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Usuario supervisor = garantirUsuario(
                "Profa. Helena Duarte", "supervisor@nutriclinica.edu.br", Perfil.SUPERVISOR);
        Usuario estagiario = garantirUsuario(
                "Marina Rocha", "estagiario@nutriclinica.edu.br", Perfil.ESTAGIARIO);
        garantirUsuario(
                "Administrador do Sistema", "admin@nutriclinica.edu.br", Perfil.ADMIN);

        garantirVinculo(supervisor, estagiario);

        log.warn("Seed de desenvolvimento aplicado: três usuários com a senha \"{}\". "
                + "Este runner não existe no perfil prod.", SENHA_DEMO);
    }

    private Usuario garantirUsuario(String nome, String email, Perfil perfil) {
        return usuarioRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            Usuario usuario = new Usuario();
            usuario.setNome(nome);
            usuario.setEmail(email);
            usuario.setSenhaHash(passwordEncoder.encode(SENHA_DEMO));
            usuario.setPerfil(perfil);
            usuario.setAtivo(true);
            return usuarioRepository.save(usuario);
        });
    }

    private void garantirVinculo(Usuario supervisor, Usuario estagiario) {
        if (vinculoSupervisaoRepository.existsBySupervisorIdAndEstagiarioIdAndAtivoTrue(
                supervisor.getId(), estagiario.getId())) {
            return;
        }

        // Pode existir desativado — reativar é o mesmo caminho do PUT de vínculos.
        VinculoSupervisao vinculo = vinculoSupervisaoRepository
                .findBySupervisorIdAndEstagiarioId(supervisor.getId(), estagiario.getId())
                .orElseGet(VinculoSupervisao::new);

        vinculo.setSupervisor(supervisor);
        vinculo.setEstagiario(estagiario);
        vinculo.setAtivo(true);
        vinculoSupervisaoRepository.save(vinculo);
    }
}
