package br.edu.nutriclinica.dto.secao;

import br.edu.nutriclinica.domain.FrequenciaAlimentar;
import br.edu.nutriclinica.domain.enums.Frequencia;

/** Schema `FrequenciaAlimentar` do contrato, no papel de resposta. */
public record FrequenciaAlimentarResponse(
        Frequencia frutas,
        Frequencia verdurasLegumes,
        Frequencia ultraprocessados,
        Frequencia refrigerante,
        Frequencia bebidaAlcoolica,
        Frequencia cafe,
        Integer aguaMlDia,
        String observacoes) {

    public static FrequenciaAlimentarResponse de(FrequenciaAlimentar secao) {
        return new FrequenciaAlimentarResponse(
                secao.getFrutas(),
                secao.getVerdurasLegumes(),
                secao.getUltraprocessados(),
                secao.getRefrigerante(),
                secao.getBebidaAlcoolica(),
                secao.getCafe(),
                secao.getAguaMlDia(),
                secao.getObservacoes());
    }
}
