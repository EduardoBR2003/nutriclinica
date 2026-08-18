package br.edu.nutriclinica.exception;

/** Credenciais ausentes, inválidas ou expiradas. Vira 401 / NAO_AUTORIZADO. */
public class NaoAutorizadoException extends RuntimeException {

    public NaoAutorizadoException(String mensagem) {
        super(mensagem);
    }
}
