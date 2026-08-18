package br.edu.nutriclinica.service.auditoria;

import br.edu.nutriclinica.config.security.UsuarioDetails;
import br.edu.nutriclinica.domain.Usuario;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

/**
 * Transforma a marca {@link Auditavel} em linha de {@code log_auditoria}.
 *
 * <p>{@code @AfterReturning}, e não {@code @Around}: só o que deu certo vira
 * trilha. Uma submissão recusada por seção incompleta não é um acesso a dado
 * sensível, é uma tentativa que o domínio barrou — e o 404 de recurso fora de
 * escopo, com mais razão ainda, não deve registrar o id que o usuário não podia
 * ver.
 *
 * <p>O que o aspecto sabe é o que a assinatura oferece: quem está autenticado,
 * de onde veio a requisição e o id do alvo. Detalhe clínico não entra aqui de
 * propósito — a trilha precisa responder "quem tocou neste prontuário e
 * quando", não repetir o conteúdo que ela deveria proteger.
 */
@Aspect
@Component
public class AuditoriaAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaAspect.class);

    private final LogAuditoriaService logAuditoriaService;

    public AuditoriaAspect(LogAuditoriaService logAuditoriaService) {
        this.logAuditoriaService = logAuditoriaService;
    }

    @AfterReturning("@annotation(auditavel)")
    public void registrar(JoinPoint ponto, Auditavel auditavel) {
        try {
            logAuditoriaService.registrar(
                    usuarioAutenticado(),
                    auditavel.acao(),
                    auditavel.entidade(),
                    primeiroId(ponto),
                    null,
                    enderecoIp());

        } catch (RuntimeException e) {
            // Falhar a auditoria não pode desfazer uma operação que já teve
            // êxito: o estagiário não perde a submissão porque o log caiu. Fica
            // no log da aplicação, que é onde alguém vai procurar.
            log.warn("Não foi possível registrar a auditoria de {} em {}: {}",
                    auditavel.acao(), auditavel.entidade(), e.getMessage());
        }
    }

    /** O alvo da ação, pela convenção do primeiro {@code Long} da assinatura. */
    private Long primeiroId(JoinPoint ponto) {
        for (Object argumento : ponto.getArgs()) {
            if (argumento instanceof Long id) {
                return id;
            }
        }
        return null;
    }

    private Usuario usuarioAutenticado() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.getPrincipal() instanceof UsuarioDetails detalhes) {
            return detalhes.getUsuario();
        }
        return null;
    }

    /**
     * Endereço de origem, tirado do {@code Authentication}.
     *
     * <p>O {@code JwtAuthenticationFilter} já preenche os detalhes da
     * autenticação com os dados da requisição, então não é preciso alcançar o
     * {@code RequestContextHolder} daqui.
     */
    private String enderecoIp() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.getDetails() instanceof WebAuthenticationDetails detalhes) {
            return detalhes.getRemoteAddress();
        }
        return null;
    }
}
