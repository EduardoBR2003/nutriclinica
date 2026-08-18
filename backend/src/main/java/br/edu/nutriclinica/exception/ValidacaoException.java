package br.edu.nutriclinica.exception;

import br.edu.nutriclinica.dto.ErroResponse;

import java.util.List;

/**
 * Falha de validação que só o domínio consegue detectar — depende do banco ou
 * de outra regra, e portanto não cabe numa anotação de Bean Validation. É o
 * caso do supervisor informado que não supervisiona o estagiário logado.
 *
 * <p>Vira 422 / VALIDACAO, o mesmo formato de
 * {@code MethodArgumentNotValidException}: para o frontend, um campo inválido é
 * um campo inválido, venha a regra de onde vier.
 */
public class ValidacaoException extends RuntimeException {

    private final List<ErroResponse.CampoErro> campos;

    public ValidacaoException(String campo, String mensagem) {
        super(mensagem);
        this.campos = List.of(new ErroResponse.CampoErro(campo, mensagem));
    }

    public List<ErroResponse.CampoErro> getCampos() {
        return campos;
    }
}
