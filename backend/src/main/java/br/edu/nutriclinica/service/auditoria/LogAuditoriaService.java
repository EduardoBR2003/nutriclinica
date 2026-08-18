package br.edu.nutriclinica.service.auditoria;

import br.edu.nutriclinica.domain.LogAuditoria;
import br.edu.nutriclinica.domain.Usuario;
import br.edu.nutriclinica.repository.LogAuditoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gravação da trilha de auditoria exigida pela LGPD.
 *
 * <p>Chamado pelo {@link AuditoriaAspect}, não pelos serviços de domínio.
 */
@Service
public class LogAuditoriaService {

    /** Limites das colunas em {@code log_auditoria}. Trunca-se, não se estoura. */
    private static final int TAMANHO_ACAO = 30;
    private static final int TAMANHO_ENTIDADE = 60;
    private static final int TAMANHO_IP = 45;

    private final LogAuditoriaRepository logAuditoriaRepository;

    public LogAuditoriaService(LogAuditoriaRepository logAuditoriaRepository) {
        this.logAuditoriaRepository = logAuditoriaRepository;
    }

    /**
     * Grava uma linha da trilha, em transação própria.
     *
     * <p>{@code REQUIRES_NEW} não é preciosismo, é o que faz a auditoria de
     * <b>leitura</b> existir. {@code AtendimentoService.detalhar} é
     * {@code @Transactional(readOnly = true)}, e nesse caso o Spring põe a sessão
     * do Hibernate em {@code FlushMode.MANUAL}: um insert emitido na mesma
     * transação nunca seria descarregado, e o registro sumiria sem erro nenhum.
     * A transação própria também é o que mantém a trilha de pé quando a operação
     * de negócio falha depois — a evidência é sobre o <i>ato de acessar</i>, que
     * aconteceu.
     *
     * <p>Segura brevemente uma segunda conexão do pool enquanto a de fora está
     * aberta. É por isso que este método é minúsculo e não chama de volta
     * nenhum serviço de domínio.
     *
     * @param usuario quem agiu; nulo em ação de sistema
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Usuario usuario, String acao, String entidade, Long entidadeId,
                          String detalhe, String enderecoIp) {
        LogAuditoria registro = new LogAuditoria();
        registro.setUsuario(usuario);
        registro.setAcao(limitar(acao, TAMANHO_ACAO));
        registro.setEntidade(limitar(entidade, TAMANHO_ENTIDADE));
        registro.setEntidadeId(entidadeId);
        registro.setDetalhe(detalhe);
        registro.setEnderecoIp(limitar(enderecoIp, TAMANHO_IP));

        logAuditoriaRepository.save(registro);
    }

    private String limitar(String valor, int tamanho) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= tamanho ? valor : valor.substring(0, tamanho);
    }
}
