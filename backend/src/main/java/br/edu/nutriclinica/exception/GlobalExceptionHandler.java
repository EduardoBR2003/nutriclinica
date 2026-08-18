package br.edu.nutriclinica.exception;

import br.edu.nutriclinica.dto.ErroResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;

/**
 * Traduz exceções para o schema `Erro` do docs/api.yaml.
 *
 * Cobre apenas o que chega ao DispatcherServlet: o que a cadeia de filtros do
 * Spring Security rejeita antes disso é tratado por ErroAuthenticationEntryPoint
 * e ErroAccessDeniedHandler, que devolvem o mesmo formato.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException excecao) {
        List<ErroResponse.CampoErro> campos = excecao.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ErroResponse.CampoErro(erro.getField(), erro.getDefaultMessage()))
                .toList();

        return ResponseEntity.unprocessableEntity()
                .body(ErroResponse.de("VALIDACAO", "Falha de validação nos dados enviados.", campos));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroResponse> tratarViolacao(ConstraintViolationException excecao) {
        List<ErroResponse.CampoErro> campos = excecao.getConstraintViolations().stream()
                .map(violacao -> new ErroResponse.CampoErro(
                        violacao.getPropertyPath().toString(), violacao.getMessage()))
                .toList();

        return ResponseEntity.unprocessableEntity()
                .body(ErroResponse.de("VALIDACAO", "Falha de validação nos dados enviados.", campos));
    }

    @ExceptionHandler({NaoAutorizadoException.class, AuthenticationException.class})
    public ResponseEntity<ErroResponse> tratarNaoAutorizado(RuntimeException excecao) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErroResponse.de("NAO_AUTORIZADO", excecao.getMessage()));
    }

    @ExceptionHandler({SemPermissaoException.class, AccessDeniedException.class})
    public ResponseEntity<ErroResponse> tratarSemPermissao(RuntimeException excecao) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErroResponse.de("SEM_PERMISSAO", excecao.getMessage()));
    }

    @ExceptionHandler({NaoEncontradoException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErroResponse> tratarNaoEncontrado(Exception excecao) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErroResponse.de("NAO_ENCONTRADO", excecao.getMessage()));
    }

    @ExceptionHandler(TransicaoInvalidaException.class)
    public ResponseEntity<ErroResponse> tratarTransicaoInvalida(TransicaoInvalidaException excecao) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErroResponse.de("TRANSICAO_INVALIDA", excecao.getMessage()));
    }

    /** Rede de segurança: detalhe do erro fica no log, nunca na resposta. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarErroInterno(Exception excecao) {
        log.error("Erro não tratado", excecao);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErroResponse.de("ERRO_INTERNO", "Erro interno no servidor."));
    }
}
