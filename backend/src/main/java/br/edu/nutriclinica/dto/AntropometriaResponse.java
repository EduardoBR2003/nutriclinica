package br.edu.nutriclinica.dto;

import br.edu.nutriclinica.domain.Antropometria;
import br.edu.nutriclinica.domain.enums.ClassificacaoImc;
import br.edu.nutriclinica.domain.enums.RiscoCardiovascular;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Schema `Antropometria` do contrato: os valores medidos mais os indicadores
 * calculados pelo servidor.
 *
 * <p>{@code imc}, {@code classificacaoImc} e {@code relacaoCinturaQuadril} são
 * lidos da entidade, onde foram gravados no salvamento. Já {@code rcqElevada} e
 * {@code riscoCardiovascular} são derivados aqui, na montagem da resposta, a
 * partir da RCQ e da cintura armazenadas e do sexo do paciente — não existem
 * como coluna.
 */
public record AntropometriaResponse(
        BigDecimal pesoKg,
        BigDecimal alturaCm,
        BigDecimal circCinturaCm,
        BigDecimal circQuadrilCm,
        BigDecimal percentualGordura,
        BigDecimal massaMagraKg,
        LocalDate aferidoEm,
        BigDecimal imc,
        ClassificacaoImc classificacaoImc,
        BigDecimal relacaoCinturaQuadril,
        Boolean rcqElevada,
        RiscoCardiovascular riscoCardiovascular) {

    /**
     * @param antropometria       seção como está gravada
     * @param rcqElevada          derivado da RCQ armazenada e do sexo do paciente
     * @param riscoCardiovascular derivado da cintura armazenada e do sexo do paciente
     */
    public static AntropometriaResponse de(Antropometria antropometria,
                                           Boolean rcqElevada,
                                           RiscoCardiovascular riscoCardiovascular) {
        return new AntropometriaResponse(
                antropometria.getPesoKg(),
                antropometria.getAlturaCm(),
                antropometria.getCircCinturaCm(),
                antropometria.getCircQuadrilCm(),
                antropometria.getPercentualGordura(),
                antropometria.getMassaMagraKg(),
                antropometria.getAferidoEm(),
                antropometria.getImc(),
                converterClassificacao(antropometria.getClassificacaoImc()),
                antropometria.getRelacaoCinturaQuadril(),
                rcqElevada,
                riscoCardiovascular);
    }

    /**
     * A coluna {@code classificacao_imc} ainda é VARCHAR e a entidade a expõe
     * como String. Um valor gravado por uma versão anterior que não exista mais
     * no enum vira {@code null} em vez de derrubar a leitura do prontuário.
     */
    private static ClassificacaoImc converterClassificacao(String classificacao) {
        if (classificacao == null || classificacao.isBlank()) {
            return null;
        }
        try {
            return ClassificacaoImc.valueOf(classificacao);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
