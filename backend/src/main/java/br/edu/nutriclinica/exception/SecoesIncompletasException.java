package br.edu.nutriclinica.exception;

import br.edu.nutriclinica.dto.ErroResponse;

import java.util.List;

/**
 * Tentativa de submeter um prontuário com seção obrigatória por preencher.
 * Vira 409 / SECOES_INCOMPLETAS.
 *
 * <p>É conflito, e não 422: o corpo da requisição de submissão está correto —
 * ela nem tem corpo. O que impede a operação é o <b>estado</b> do prontuário,
 * exatamente como em {@link TransicaoInvalidaException}.
 *
 * <p>Carrega em {@code campos} um item por campo faltante, com o caminho no
 * estilo do contrato ({@code antropometria.alturaCm}). É o que permite ao
 * frontend levar o estagiário direto ao que falta, em vez de mostrar um aviso
 * genérico e deixá-lo procurar pelas onze seções.
 */
public class SecoesIncompletasException extends ConflitoException {

    public static final String CODIGO = "SECOES_INCOMPLETAS";

    public SecoesIncompletasException(List<ErroResponse.CampoErro> campos) {
        super(CODIGO,
                "O prontuário não pode ser submetido: há seções obrigatórias incompletas.",
                campos);
    }
}
