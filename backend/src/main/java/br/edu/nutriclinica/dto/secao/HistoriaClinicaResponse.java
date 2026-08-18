package br.edu.nutriclinica.dto.secao;

import br.edu.nutriclinica.domain.HistoriaClinica;
import br.edu.nutriclinica.domain.enums.HabitoIntestinal;
import br.edu.nutriclinica.domain.enums.NivelEstresse;
import br.edu.nutriclinica.domain.enums.QualidadeSono;
import br.edu.nutriclinica.domain.enums.Tabagismo;

import java.math.BigDecimal;

/** Schema `HistoriaClinica` do contrato, no papel de resposta. */
public record HistoriaClinicaResponse(
        String doencasDiagnosticadas,
        String alergias,
        String intolerancias,
        String historicoFamiliar,
        Tabagismo tabagismo,
        BigDecimal horasSono,
        QualidadeSono qualidadeSono,
        NivelEstresse nivelEstresse,
        HabitoIntestinal habitoIntestinal,
        Boolean praticaAtividadeFisica,
        String descricaoAtividade,
        String observacoes) {

    public static HistoriaClinicaResponse de(HistoriaClinica secao) {
        return new HistoriaClinicaResponse(
                secao.getDoencasDiagnosticadas(),
                secao.getAlergias(),
                secao.getIntolerancias(),
                secao.getHistoricoFamiliar(),
                secao.getTabagismo(),
                secao.getHorasSono(),
                secao.getQualidadeSono(),
                secao.getNivelEstresse(),
                secao.getHabitoIntestinal(),
                secao.getPraticaAtividadeFisica(),
                secao.getDescricaoAtividade(),
                secao.getObservacoes());
    }
}
