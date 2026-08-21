package br.edu.nutriclinica.config.seed;

import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.domain.enums.Perfil;
import br.edu.nutriclinica.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O seed que saiu do Flyway continua entregando, no perfil it, o mesmo cenário
 * que o V2 entregava — é dele que dependem os outros testes de integração.
 */
class SeedDesenvolvimentoRunnerIT extends AbstractIntegrationTest {

    @Autowired
    private SeedDesenvolvimentoRunner runner;

    @Test
    @DisplayName("os três usuários existem, ativos e com a senha de demonstração")
    void semeiaOsTresPerfis() throws Exception {
        Usuario admin = usuarioPorEmail(ADMIN);
        Usuario supervisor = usuarioPorEmail(SUPERVISOR);
        Usuario estagiario = usuarioPorEmail(ESTAGIARIO);

        assertThat(admin.getPerfil()).isEqualTo(Perfil.ADMIN);
        assertThat(supervisor.getPerfil()).isEqualTo(Perfil.SUPERVISOR);
        assertThat(estagiario.getPerfil()).isEqualTo(Perfil.ESTAGIARIO);
        assertThat(admin.isAtivo()).isTrue();

        // A senha só é de fato utilizável se o hash foi gerado pelo encoder.
        assertThat(token(ADMIN)).isNotBlank();
    }

    @Test
    @DisplayName("o vínculo do supervisor com o estagiário vem pronto")
    void semeiaOVinculo() {
        assertThat(vinculoSupervisaoRepository.existsBySupervisorIdAndEstagiarioIdAndAtivoTrue(
                usuarioPorEmail(SUPERVISOR).getId(),
                usuarioPorEmail(ESTAGIARIO).getId())).isTrue();
    }

    @Test
    @DisplayName("rodar de novo não duplica: a guarda é o e-mail, não a tabela vazia")
    void ehIdempotente() {
        long antes = usuarioRepository.count();

        runner.run();
        runner.run();

        assertThat(usuarioRepository.count()).isEqualTo(antes);
    }
}
