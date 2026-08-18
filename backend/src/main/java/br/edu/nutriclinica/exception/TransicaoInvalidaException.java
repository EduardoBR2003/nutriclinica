package br.edu.nutriclinica.exception;

/**
 * A operação não é válida no estado atual do atendimento — por exemplo, editar
 * uma seção em EM_REVISAO. Vira 409 / TRANSICAO_INVALIDA.
 */
public class TransicaoInvalidaException extends ConflitoException {

    public static final String CODIGO = "TRANSICAO_INVALIDA";

    public TransicaoInvalidaException(String mensagem) {
        super(CODIGO, mensagem);
    }
}
