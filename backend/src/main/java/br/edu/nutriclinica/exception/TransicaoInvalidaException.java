package br.edu.nutriclinica.exception;

/**
 * A operação não é válida no estado atual do atendimento — por exemplo, editar
 * uma seção em EM_REVISAO ou abrir atendimento para paciente sem termo LGPD.
 * Vira 409 / TRANSICAO_INVALIDA.
 */
public class TransicaoInvalidaException extends RuntimeException {

    public TransicaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
