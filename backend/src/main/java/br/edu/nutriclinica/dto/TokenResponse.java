package br.edu.nutriclinica.dto;

/** Schema `TokenResponse` do contrato. `expiraEm` é o TTL do access token em segundos. */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiraEm,
        UsuarioResponse usuario) {
}
