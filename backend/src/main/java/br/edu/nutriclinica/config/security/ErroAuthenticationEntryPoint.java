package br.edu.nutriclinica.config.security;

import br.edu.nutriclinica.dto.ErroResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Requisição sem autenticação válida em rota protegida.
 *
 * Necessário porque as exceções do Spring Security acontecem na cadeia de filtros,
 * antes do DispatcherServlet, e portanto não passam pelo GlobalExceptionHandler.
 */
@Component
public class ErroAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public ErroAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest requisicao,
                         HttpServletResponse resposta,
                         AuthenticationException excecao) throws IOException {

        resposta.setStatus(HttpStatus.UNAUTHORIZED.value());
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(resposta.getOutputStream(),
                ErroResponse.de("NAO_AUTORIZADO", "Autenticação necessária para acessar este recurso."));
    }
}
