package br.edu.nutriclinica.exception;

/** Recurso inexistente. Vira 404 / NAO_ENCONTRADO. */
public class NaoEncontradoException extends RuntimeException {

    public NaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public static NaoEncontradoException de(String recurso, Object id) {
        return new NaoEncontradoException(recurso + " " + id + " não encontrado.");
    }
}
