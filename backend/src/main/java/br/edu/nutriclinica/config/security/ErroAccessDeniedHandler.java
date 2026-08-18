package br.edu.nutriclinica.config.security;

import br.edu.nutriclinica.dto.ErroResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Usuário autenticado, mas sem permissão para a rota. Ver {@link ErroAuthenticationEntryPoint}. */
@Component
public class ErroAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public ErroAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest requisicao,
                       HttpServletResponse resposta,
                       AccessDeniedException excecao) throws IOException {

        resposta.setStatus(HttpStatus.FORBIDDEN.value());
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(resposta.getOutputStream(),
                ErroResponse.de("SEM_PERMISSAO", "Você não tem permissão para acessar este recurso."));
    }
}
