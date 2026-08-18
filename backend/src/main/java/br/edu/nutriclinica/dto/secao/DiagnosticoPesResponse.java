package br.edu.nutriclinica.dto.secao;

import br.edu.nutriclinica.domain.DiagnosticoPes;

/** Schema `DiagnosticoPes` do contrato, no papel de resposta. */
public record DiagnosticoPesResponse(
        String problema,
        String etiologia,
        String sinaisSintomas) {

    public static DiagnosticoPesResponse de(DiagnosticoPes secao) {
        return new DiagnosticoPesResponse(
                secao.getProblema(),
                secao.getEtiologia(),
                secao.getSinaisSintomas());
    }
}
