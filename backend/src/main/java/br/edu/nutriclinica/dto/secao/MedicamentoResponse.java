package br.edu.nutriclinica.dto.secao;

import br.edu.nutriclinica.domain.Medicamento;
import br.edu.nutriclinica.domain.enums.TipoMedicamento;

/** Schema `Medicamento` do contrato, no papel de resposta. */
public record MedicamentoResponse(
        Long id,
        TipoMedicamento tipo,
        String nome,
        String dose,
        String frequencia) {

    public static MedicamentoResponse de(Medicamento medicamento) {
        return new MedicamentoResponse(
                medicamento.getId(),
                medicamento.getTipo(),
                medicamento.getNome(),
                medicamento.getDose(),
                medicamento.getFrequencia());
    }
}
