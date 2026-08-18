package br.edu.nutriclinica.dto.secao;

import br.edu.nutriclinica.domain.QueixaPrincipal;

/** Schema `QueixaPrincipal` do contrato, no papel de resposta. */
public record QueixaPrincipalResponse(
        String motivo,
        String tempoQueixa,
        String tratamentoAnterior,
        String objetivoConsulta,
        String observacoes) {

    public static QueixaPrincipalResponse de(QueixaPrincipal secao) {
        return new QueixaPrincipalResponse(
                secao.getMotivo(),
                secao.getTempoQueixa(),
                secao.getTratamentoAnterior(),
                secao.getObjetivoConsulta(),
                secao.getObservacoes());
    }
}
