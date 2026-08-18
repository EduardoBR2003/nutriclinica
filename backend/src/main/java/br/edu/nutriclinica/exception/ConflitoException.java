package br.edu.nutriclinica.exception;

/**
 * A operação não é válida no estado atual do recurso. Vira 409, com o código
 * que a subclasse define.
 *
 * <p>O contrato usa 409 para situações distintas — {@code TRANSICAO_INVALIDA} ao
 * editar um prontuário fechado, {@code SEM_TERMO_CONSENTIMENTO} ao abrir
 * atendimento sem termo LGPD — e o frontend distingue as duas pelo {@code codigo},
 * não pelo texto da mensagem. Por isso o código viaja na exceção.
 */
public class ConflitoException extends RuntimeException {

    private final String codigo;

    public ConflitoException(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
