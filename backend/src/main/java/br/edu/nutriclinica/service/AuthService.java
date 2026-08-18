package br.edu.nutriclinica.service;

import br.edu.nutriclinica.config.security.UsuarioDetails;
import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.dto.LoginRequest;
import br.edu.nutriclinica.dto.RefreshRequest;
import br.edu.nutriclinica.dto.TokenResponse;
import br.edu.nutriclinica.dto.UsuarioResponse;
import br.edu.nutriclinica.exception.NaoAutorizadoException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public TokenResponse login(LoginRequest requisicao) {
        Authentication autenticacao;
        try {
            autenticacao = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(requisicao.email(), requisicao.senha()));
        } catch (AuthenticationException e) {
            // Mensagem genérica de propósito: não revela se o e-mail existe.
            throw new NaoAutorizadoException("E-mail ou senha inválidos.");
        }

        Usuario usuario = ((UsuarioDetails) autenticacao.getPrincipal()).getUsuario();
        return emitirTokens(usuario);
    }

    /**
     * Rotação: o refresh token apresentado é revogado e um par novo é emitido.
     * Reapresentar o token antigo passa a resultar em 401.
     */
    @Transactional
    public TokenResponse refresh(RefreshRequest requisicao) {
        Usuario usuario = refreshTokenService.validarEConsumir(requisicao.refreshToken());

        if (!usuario.isAtivo()) {
            throw new NaoAutorizadoException("Usuário inativo.");
        }
        return emitirTokens(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse usuarioAutenticado() {
        return UsuarioResponse.de(usuarioLogado());
    }

    /** Usuário da requisição atual. Os próximos blocos usam isto para aplicar as regras de LGPD. */
    public Usuario usuarioLogado() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !(autenticacao.getPrincipal() instanceof UsuarioDetails detalhes)) {
            throw new NaoAutorizadoException("Nenhum usuário autenticado na requisição.");
        }
        return detalhes.getUsuario();
    }

    private TokenResponse emitirTokens(Usuario usuario) {
        return new TokenResponse(
                jwtService.gerarAccessToken(usuario),
                refreshTokenService.emitir(usuario).getToken(),
                jwtService.getExpiraEmSegundos(),
                UsuarioResponse.de(usuario));
    }
}
