package br.edu.nutriclinica.exception;

/**
 * O usuário está autenticado, mas o recurso não pertence ao seu escopo
 * (LGPD: estagiário só vê os próprios pacientes, supervisor só os dos seus
 * orientados). Vira 403 / SEM_PERMISSAO.
 */
public class SemPermissaoException extends RuntimeException {

    public SemPermissaoException(String mensagem) {
        super(mensagem);
    }
}
