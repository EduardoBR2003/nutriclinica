package br.edu.nutriclinica.dto.secao;

import br.edu.nutriclinica.domain.ComportamentoAlimentar;

/** Schema `ComportamentoAlimentar` do contrato, no papel de resposta. */
public record ComportamentoAlimentarResponse(
        Boolean comeAssistindoTela,
        Boolean comeRapido,
        Boolean pulaRefeicoes,
        Boolean compulsaoAlimentar,
        Boolean alimentacaoEmocional,
        Boolean restricaoAlimentar,
        String observacoes) {

    public static ComportamentoAlimentarResponse de(ComportamentoAlimentar secao) {
        return new ComportamentoAlimentarResponse(
                secao.getComeAssistindoTela(),
                secao.getComeRapido(),
                secao.getPulaRefeicoes(),
                secao.getCompulsaoAlimentar(),
                secao.getAlimentacaoEmocional(),
                secao.getRestricaoAlimentar(),
                secao.getObservacoes());
    }
}
