package br.edu.nutriclinica.dto.secao;

import br.edu.nutriclinica.domain.ExameBioquimico;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Schema `ExameBioquimico` do contrato, no papel de resposta. */
public record ExameBioquimicoResponse(
        Long id,
        String nomeExame,
        BigDecimal valor,
        String unidade,
        LocalDate dataExame,
        String valorReferencia) {

    public static ExameBioquimicoResponse de(ExameBioquimico exame) {
        return new ExameBioquimicoResponse(
                exame.getId(),
                exame.getNomeExame(),
                exame.getValor(),
                exame.getUnidade(),
                exame.getDataExame(),
                exame.getValorReferencia());
    }
}
