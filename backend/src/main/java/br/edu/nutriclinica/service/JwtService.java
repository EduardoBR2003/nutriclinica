package br.edu.nutriclinica.service;

import br.edu.nutriclinica.config.AppProperties;
import br.edu.nutriclinica.domain.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Emissão e leitura do access token (HS256, 15 minutos).
 *
 * Claims: subject = id do usuário, mais `email` e `perfil`. Serviço puro — não
 * conhece HttpServletRequest nem SecurityContext, o que o torna testável isolado.
 */
@Service
public class JwtService {

    private final SecretKey chave;
    private final Duration validadeAccessToken;

    public JwtService(AppProperties propriedades) {
        // hmacShaKeyFor exige no mínimo 32 bytes para HS256. O algoritmo é fixado
        // explicitamente na assinatura: sem isso o jjwt o inferiria do tamanho da
        // chave, e o mesmo código emitiria HS384/HS512 dependendo do JWT_SECRET.
        this.chave = Keys.hmacShaKeyFor(propriedades.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.validadeAccessToken = Duration.ofMinutes(propriedades.jwt().accessTokenMinutos());
    }

    public String gerarAccessToken(Usuario usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("email", usuario.getEmail())
                .claim("perfil", usuario.getPerfil().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(validadeAccessToken)))
                .signWith(chave, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Valida a assinatura e a expiração, devolvendo as claims.
     * Lança {@link io.jsonwebtoken.JwtException} se o token for inválido — quem chama decide o que fazer.
     */
    public Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** TTL do access token em segundos — alimenta o campo `expiraEm` do contrato. */
    public long getExpiraEmSegundos() {
        return validadeAccessToken.toSeconds();
    }
}
