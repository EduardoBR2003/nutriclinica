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
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    /**
     * Violação encontrada na validação de parâmetro de método — é por aqui que
     * caem os itens de um PUT de coleção, cujas constraints estão no elemento da
     * lista e não no corpo como um todo.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroResponse> tratarViolacao(ConstraintViolationException excecao) {
        List<ErroResponse.CampoErro> campos = excecao.getConstraintViolations().stream()
                .map(violacao -> new ErroResponse.CampoErro(
                        nomeDoCampo(violacao.getPropertyPath().toString()), violacao.getMessage()))
                .toList();

        return ResponseEntity.unprocessableEntity()
                .body(ErroResponse.de("VALIDACAO", "Falha de validação nos dados enviados.", campos));
    }

    /**
     * O mesmo 422, quando quem valida os parâmetros é o Spring MVC em vez do
     * proxy de método. Qual dos dois caminhos roda depende de detalhe de
     * configuração; o que o cliente recebe não pode depender disso.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErroResponse> tratarValidacaoDeParametro(HandlerMethodValidationException excecao) {
        List<ErroResponse.CampoErro> campos = excecao.getAllValidationResults().stream()
                .flatMap(resultado -> resultado.getResolvableErrors().stream()
                        .map(erro -> new ErroResponse.CampoErro(
                                resultado.getMethodParameter().getParameterName(),
                                erro.getDefaultMessage())))
                .toList();

        return ResponseEntity.unprocessableEntity()
                .body(ErroResponse.de("VALIDACAO", "Falha de validação nos dados enviados.", campos));
    }

    /**
     * O caminho da violação inclui o nome do método que validou
     * ({@code substituir.requisicao[0].nome}). O cliente só se interessa pelo
     * campo, então o prefixo do método sai.
     */
    private String nomeDoCampo(String caminho) {
        int primeiroPonto = caminho.indexOf('.');
        return primeiroPonto < 0 ? caminho : caminho.substring(primeiroPonto + 1);
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

    /**
     * Caminho que passou pelo filtro de segurança (rota pública) mas não existe
     * no MVC. O caso concreto é o Swagger em produção: {@code /swagger-ui.html}
     * e {@code /v3/api-docs} continuam liberados no SecurityConfig, e com o
     * springdoc desligado eles chegariam à rede de segurança lá embaixo — 500 e
     * um stack trace no log a cada varredura de robô, para uma rota que
     * simplesmente não está no ar. A mensagem é fixa: a da exceção nomeia o
     * recurso estático procurado.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> tratarRecursoInexistente(NoResourceFoundException excecao) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErroResponse.de("NAO_ENCONTRADO", "Recurso não encontrado."));
    }

    /**
     * Cobre TransicaoInvalidaException, SemTermoConsentimentoException e as que
     * vierem: o código específico viaja na exceção, e não há um handler novo a
     * cada situação de conflito.
     *
     * <p>O {@code campos} vem junto pelo mesmo motivo — é
     * {@code SECOES_INCOMPLETAS} quem o preenche, listando o que falta para
     * submeter. Nos conflitos que não têm o que apontar ele é nulo, e o
     * {@code @JsonInclude(NON_NULL)} do {@link ErroResponse} o omite: a resposta
     * dos 409 anteriores continua idêntica.
     */
    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErroResponse> tratarConflito(ConflitoException excecao) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErroResponse.de(excecao.getCodigo(), excecao.getMessage(), excecao.getCampos()));
    }

    /** Validação de regra de domínio, no mesmo formato da validação de campo. */
    @ExceptionHandler(ValidacaoException.class)
    public ResponseEntity<ErroResponse> tratarValidacaoDeDominio(ValidacaoException excecao) {
        return ResponseEntity.unprocessableEntity()
                .body(ErroResponse.de("VALIDACAO", excecao.getMessage(), excecao.getCampos()));
    }

    /** Rede de segurança: detalhe do erro fica no log, nunca na resposta. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarErroInterno(Exception excecao) {
        log.error("Erro não tratado", excecao);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErroResponse.de("ERRO_INTERNO", "Erro interno no servidor."));
    }
}
