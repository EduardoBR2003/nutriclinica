package br.edu.nutriclinica.exception;

import br.edu.nutriclinica.dto.ErroResponse;

import java.util.List;

/**
 * A operação não é válida no estado atual do recurso. Vira 409, com o código
 * que a subclasse define.
 *
 * <p>O contrato usa 409 para situações distintas — {@code TRANSICAO_INVALIDA} ao
 * editar um prontuário fechado, {@code SEM_TERMO_CONSENTIMENTO} ao abrir
 * atendimento sem termo LGPD — e o frontend distingue as duas pelo {@code codigo},
 * não pelo texto da mensagem. Por isso o código viaja na exceção.
 *
 * <p>Alguns conflitos precisam dizer <b>o que</b> impede a operação, e não só que
 * ela é impossível: {@code SECOES_INCOMPLETAS} lista os campos que faltam para
 * submeter. Por isso o {@code campos} do schema {@code Erro} também viaja aqui,
 * no mesmo formato dos 422 de validação — para o frontend, um campo pendente é um
 * campo pendente, venha a regra de onde vier. Fica nulo nos conflitos que não têm
 * o que apontar, e o {@code @JsonInclude(NON_NULL)} do {@link ErroResponse} o
 * omite do JSON.
 */
public class ConflitoException extends RuntimeException {

    private final String codigo;
    private final List<ErroResponse.CampoErro> campos;

    public ConflitoException(String codigo, String mensagem) {
        this(codigo, mensagem, null);
    }

    public ConflitoException(String codigo, String mensagem, List<ErroResponse.CampoErro> campos) {
        super(mensagem);
        this.codigo = codigo;
        this.campos = campos;
    }

    public String getCodigo() {
        return codigo;
    }

    /** @return os campos que motivaram o conflito, ou {@code null} se não há o que apontar */
    public List<ErroResponse.CampoErro> getCampos() {
        return campos;
    }
}
