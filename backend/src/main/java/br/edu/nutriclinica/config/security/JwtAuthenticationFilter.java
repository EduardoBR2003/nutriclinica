package br.edu.nutriclinica.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import br.edu.nutriclinica.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lê o header `Authorization: Bearer <token>` e popula o SecurityContext.
 *
 * Token ausente, malformado ou expirado não lança exceção aqui: o contexto fica
 * vazio e quem responde 401 no formato `Erro` é o {@link ErroAuthenticationEntryPoint}.
 * Assim as rotas públicas seguem funcionando mesmo que venha um header inválido.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String PREFIXO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioDetailsService usuarioDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioDetailsService usuarioDetailsService) {
        this.jwtService = jwtService;
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest requisicao,
                                    @NonNull HttpServletResponse resposta,
                                    @NonNull FilterChain cadeia) throws ServletException, IOException {

        String token = extrairToken(requisicao);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            autenticar(token, requisicao);
        }
        cadeia.doFilter(requisicao, resposta);
    }

    private void autenticar(String token, HttpServletRequest requisicao) {
        try {
            Claims claims = jwtService.extrairClaims(token);
            UsuarioDetails detalhes = usuarioDetailsService.carregarPorId(Long.valueOf(claims.getSubject()));

            if (!detalhes.isEnabled()) {
                log.debug("Token de usuário inativo recusado: {}", detalhes.getUsername());
                return;
            }

            var autenticacao = new UsernamePasswordAuthenticationToken(
                    detalhes, null, detalhes.getAuthorities());
            autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(requisicao));
            SecurityContextHolder.getContext().setAuthentication(autenticacao);

        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            // Contexto segue vazio; o entry point responde 401.
            log.debug("Token JWT recusado: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
    }

    private String extrairToken(HttpServletRequest requisicao) {
        String header = requisicao.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIXO_BEARER)) {
            String token = header.substring(PREFIXO_BEARER.length()).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }
}
