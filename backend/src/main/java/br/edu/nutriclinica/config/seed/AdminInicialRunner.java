package br.edu.nutriclinica.config.seed;

import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.domain.enums.Perfil;
import br.edu.nutriclinica.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria o administrador inicial em produção — e só ele.
 *
 * <p>Por que não é uma migration: o Flyway executa SQL, e BCrypt não existe em
 * SQL. Um INSERT com a senha vinda de placeholder gravaria texto claro na
 * coluna {@code senha_hash}, e o login nunca funcionaria; fazer o hash no banco
 * exigiria pgcrypto, o que significaria uma segunda implementação de hash fora
 * do {@link PasswordEncoder} da aplicação, com outro custo de trabalho e outra
 * versão de BCrypt. Aqui a senha é cifrada pelo mesmo encoder que valida o
 * login, e o texto claro não atravessa o SQL.
 *
 * <p>A guarda é a tabela vazia, não a ausência daquele e-mail: se já existe
 * gente cadastrada, o sistema está em uso e recriar um administrador a cada
 * partida seria uma porta dos fundos permanente — bastaria alterar a variável
 * de ambiente para ganhar acesso.
 */
@Component
@Profile("prod")
@EnableConfigurationProperties(AdminProperties.class)
public class AdminInicialRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicialRunner.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties propriedades;

    public AdminInicialRunner(UsuarioRepository usuarioRepository,
                              PasswordEncoder passwordEncoder,
                              AdminProperties propriedades) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.propriedades = propriedades;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            log.info("Base já tem usuários; administrador inicial não será criado.");
            return;
        }

        Usuario admin = new Usuario();
        admin.setNome("Administrador");
        admin.setEmail(propriedades.email().trim().toLowerCase());
        admin.setSenhaHash(passwordEncoder.encode(propriedades.senha()));
        admin.setPerfil(Perfil.ADMIN);
        admin.setAtivo(true);

        usuarioRepository.save(admin);

        // O e-mail é registrado; a senha, nunca — ela só existe na variável de
        // ambiente e no hash.
        log.info("Administrador inicial criado para {}. Troque a senha no primeiro acesso.",
                admin.getEmail());
    }
}
