package br.edu.nutriclinica.config.seed;

import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.domain.enums.Perfil;
import br.edu.nutriclinica.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * O administrador inicial de produção, sem subir contexto: o que importa aqui é
 * a decisão de criar ou não, e o que vai gravado.
 */
@ExtendWith(MockitoExtension.class)
class AdminInicialRunnerTest {

    private static final String EMAIL = "Coordenacao@Instituicao.edu.br";
    private static final String SENHA = "senha-de-producao-123";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminInicialRunner runnerCom(PasswordEncoder encoder) {
        return new AdminInicialRunner(usuarioRepository, encoder,
                new AdminProperties(EMAIL, SENHA));
    }

    @Test
    @DisplayName("base vazia: cria o ADMIN ativo, com e-mail normalizado e senha cifrada")
    void criaComBaseVazia() {
        when(usuarioRepository.count()).thenReturn(0L);
        // Encoder real: o teste prova que o que foi gravado casa com a senha,
        // e não apenas que algum método de cifra foi chamado.
        PasswordEncoder encoder = new BCryptPasswordEncoder();

        runnerCom(encoder).run();

        ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(capturado.capture());
        Usuario admin = capturado.getValue();

        assertThat(admin.getPerfil()).isEqualTo(Perfil.ADMIN);
        assertThat(admin.isAtivo()).isTrue();
        assertThat(admin.getEmail()).isEqualTo("coordenacao@instituicao.edu.br");
        assertThat(admin.getSenhaHash()).isNotEqualTo(SENHA);
        assertThat(encoder.matches(SENHA, admin.getSenhaHash())).isTrue();
    }

    @Test
    @DisplayName("base com usuários: não cria nada — não é porta dos fundos por variável de ambiente")
    void naoCriaComBasePovoada() {
        when(usuarioRepository.count()).thenReturn(7L);

        runnerCom(passwordEncoder).run();

        verify(usuarioRepository, never()).save(any());
    }
}
