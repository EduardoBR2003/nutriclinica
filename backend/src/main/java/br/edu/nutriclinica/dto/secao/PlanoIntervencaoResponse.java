package br.edu.nutriclinica.dto.secao;

import br.edu.nutriclinica.domain.PlanoIntervencao;

import java.math.BigDecimal;

/** Schema `PlanoIntervencao` do contrato, no papel de resposta. */
public record PlanoIntervencaoResponse(
        String objetivos,
        Integer prescricaoEnergeticaKcal,
        BigDecimal percCarboidrato,
        BigDecimal percProteina,
        BigDecimal percLipideo,
        String estrategiasComportamentais,
        String educacaoAlimentar) {

    public static PlanoIntervencaoResponse de(PlanoIntervencao secao) {
        return new PlanoIntervencaoResponse(
                secao.getObjetivos(),
                secao.getPrescricaoEnergeticaKcal(),
                secao.getPercCarboidrato(),
                secao.getPercProteina(),
                secao.getPercLipideo(),
                secao.getEstrategiasComportamentais(),
                secao.getEducacaoAlimentar());
    }
}
