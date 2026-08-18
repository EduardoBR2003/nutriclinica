package br.edu.nutriclinica.service;

import br.edu.nutriclinica.config.AppProperties;
import br.edu.nutriclinica.domain.RefreshToken;
import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.exception.NaoAutorizadoException;
import br.edu.nutriclinica.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Refresh token opaco: 32 bytes aleatórios em Base64URL. Não carrega informação —
 * validade, revogação e dono vêm da linha em `refresh_token`. Isso permite revogar
 * de verdade, coisa que um token assinado autocontido não permitiria.
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final RefreshTokenRepository refreshTokenRepository;
    private final long validadeDias;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, AppProperties propriedades) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.validadeDias = propriedades.jwt().refreshTokenDias();
    }

    @Transactional
    public RefreshToken emitir(Usuario usuario) {
        RefreshToken token = new RefreshToken();
        token.setUsuario(usuario);
        token.setToken(gerarValorOpaco());
        token.setExpiraEm(LocalDateTime.now().plusDays(validadeDias));
        token.setRevogado(false);
        return refreshTokenRepository.save(token);
    }

    /**
     * Consome o token na rotação: valida e já o marca como revogado, de modo que
     * um mesmo refresh token nunca serve duas vezes.
     */
    @Transactional
    public Usuario validarEConsumir(String valor) {
        RefreshToken token = refreshTokenRepository.findByToken(valor)
                .orElseThrow(() -> new NaoAutorizadoException("Refresh token inválido."));

        if (token.isRevogado()) {
            throw new NaoAutorizadoException("Refresh token já utilizado ou revogado.");
        }
        if (token.getExpiraEm().isBefore(LocalDateTime.now())) {
            throw new NaoAutorizadoException("Refresh token expirado.");
        }

        token.setRevogado(true);
        refreshTokenRepository.save(token);
        return token.getUsuario();
    }

    @Transactional
    public void revogarTodosDoUsuario(Long usuarioId) {
        refreshTokenRepository.deleteByUsuarioId(usuarioId);
    }

    private String gerarValorOpaco() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }
}
