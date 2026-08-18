package br.edu.nutriclinica.service;

import br.edu.nutriclinica.domain.enums.ClassificacaoImc;
import br.edu.nutriclinica.domain.enums.RiscoCardiovascular;
import br.edu.nutriclinica.domain.enums.Sexo;
import br.edu.nutriclinica.dto.ResultadoAntropometrico;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cálculo dos indicadores antropométricos derivados.
 *
 * <p>Serviço puro: não acessa banco nem depende de outros beans. Recebe os
 * valores medidos, o sexo e a idade do paciente na data da consulta, e devolve
 * os indicadores. O cliente jamais envia valores derivados.
 *
 * <p>Implementa {@code docs/regras-calculo.md}, que é a fonte da verdade dos
 * pontos de corte. Todos os cálculos usam {@link BigDecimal} com
 * {@link RoundingMode#HALF_UP}: em {@code double}, um IMC de 25 vira
 * {@code 24.999999999997} e classifica errado no limite da faixa.
 */
@Service
public class CalculoAntropometricoService {

    private static final int ESCALA = 2;

    private static final BigDecimal CEM = new BigDecimal("100");

    /** Idade mínima para classificar pelo IMC absoluto. Abaixo disso é escore-z. */
    private static final int IDADE_MINIMA_CLASSIFICACAO = 20;
    /** A partir daqui aplica-se Lipschitz em vez da tabela da OMS. */
    private static final int IDADE_IDOSO = 60;

    // Pontos de corte do IMC — OMS, adultos de 20 a 59 anos.
    private static final BigDecimal IMC_BAIXO_PESO = new BigDecimal("18.50");
    private static final BigDecimal IMC_SOBREPESO = new BigDecimal("25.00");
    private static final BigDecimal IMC_OBESIDADE_I = new BigDecimal("30.00");
    private static final BigDecimal IMC_OBESIDADE_II = new BigDecimal("35.00");
    private static final BigDecimal IMC_OBESIDADE_III = new BigDecimal("40.00");

    // Pontos de corte do IMC — Lipschitz, idosos.
    private static final BigDecimal IMC_IDOSO_BAIXO_PESO = new BigDecimal("22.00");
    private static final BigDecimal IMC_IDOSO_SOBREPESO = new BigDecimal("27.00");

    // Relação cintura/quadril com risco elevado — WHO 2008.
    private static final BigDecimal RCQ_ELEVADA_MASCULINO = new BigDecimal("0.90");
    private static final BigDecimal RCQ_ELEVADA_FEMININO = new BigDecimal("0.85");

    // Circunferência da cintura (cm) — WHO 2008.
    private static final BigDecimal CINTURA_AUMENTADO_MASCULINO = new BigDecimal("94");
    private static final BigDecimal CINTURA_MUITO_AUMENTADO_MASCULINO = new BigDecimal("102");
    private static final BigDecimal CINTURA_AUMENTADO_FEMININO = new BigDecimal("80");
    private static final BigDecimal CINTURA_MUITO_AUMENTADO_FEMININO = new BigDecimal("88");

    /**
     * Calcula todos os indicadores derivados de uma aferição.
     *
     * <p>Nenhum argumento é obrigatório. Dado ausente resulta em {@code null}
     * no indicador correspondente, nunca em exceção.
     *
     * @param pesoKg        peso corporal em quilogramas
     * @param alturaCm      altura em centímetros, como é armazenada
     * @param circCinturaCm circunferência da cintura em centímetros
     * @param circQuadrilCm circunferência do quadril em centímetros
     * @param sexo          sexo do paciente; {@code OUTRO} e {@code NAO_INFORMADO}
     *                      não têm ponto de corte definido
     * @param idadeAnos     idade do paciente <b>na data da consulta</b>
     */
    public ResultadoAntropometrico calcular(BigDecimal pesoKg,
                                            BigDecimal alturaCm,
                                            BigDecimal circCinturaCm,
                                            BigDecimal circQuadrilCm,
                                            Sexo sexo,
                                            Integer idadeAnos) {

        BigDecimal imc = calcularImc(pesoKg, alturaCm);
        BigDecimal rcq = calcularRelacaoCinturaQuadril(circCinturaCm, circQuadrilCm);

        return new ResultadoAntropometrico(
                imc,
                classificarImc(imc, idadeAnos),
                rcq,
                avaliarRcqElevada(rcq, sexo),
                avaliarRiscoCardiovascular(circCinturaCm, sexo)
        );
    }

    /**
     * IMC = peso_kg / (altura_m)². Altura chega em centímetros e é convertida
     * antes da divisão.
     *
     * @return IMC com 2 casas decimais, ou {@code null} se faltar peso ou altura
     */
    public BigDecimal calcularImc(BigDecimal pesoKg, BigDecimal alturaCm) {
        if (pesoKg == null || !ehPositivo(alturaCm)) {
            return null;
        }
        BigDecimal alturaM = alturaCm.divide(CEM, alturaCm.scale() + 2, RoundingMode.HALF_UP);
        return pesoKg.divide(alturaM.multiply(alturaM), ESCALA, RoundingMode.HALF_UP);
    }

    /**
     * Faixa do IMC conforme a idade na data da consulta.
     *
     * @return {@code null} se o IMC ou a idade forem desconhecidos, ou se o
     *         paciente tiver menos de 20 anos — nessa faixa a avaliação usa
     *         escore-z das curvas da OMS, não implementado nesta versão
     */
    public ClassificacaoImc classificarImc(BigDecimal imc, Integer idadeAnos) {
        if (imc == null || idadeAnos == null || idadeAnos < IDADE_MINIMA_CLASSIFICACAO) {
            return null;
        }
        return idadeAnos >= IDADE_IDOSO ? classificarImcIdoso(imc) : classificarImcAdulto(imc);
    }

    /** Pontos de corte da OMS para 20 a 59 anos. */
    private ClassificacaoImc classificarImcAdulto(BigDecimal imc) {
        if (imc.compareTo(IMC_BAIXO_PESO) < 0) {
            return ClassificacaoImc.BAIXO_PESO;
        }
        if (imc.compareTo(IMC_SOBREPESO) < 0) {
            return ClassificacaoImc.EUTROFIA;
        }
        if (imc.compareTo(IMC_OBESIDADE_I) < 0) {
            return ClassificacaoImc.SOBREPESO;
        }
        if (imc.compareTo(IMC_OBESIDADE_II) < 0) {
            return ClassificacaoImc.OBESIDADE_GRAU_I;
        }
        if (imc.compareTo(IMC_OBESIDADE_III) < 0) {
            return ClassificacaoImc.OBESIDADE_GRAU_II;
        }
        return ClassificacaoImc.OBESIDADE_GRAU_III;
    }

    /**
     * Pontos de corte de Lipschitz para 60 anos ou mais. A faixa de eutrofia é
     * fechada nos dois extremos: 27,00 ainda é eutrofia, 27,01 já é sobrepeso.
     */
    private ClassificacaoImc classificarImcIdoso(BigDecimal imc) {
        if (imc.compareTo(IMC_IDOSO_BAIXO_PESO) < 0) {
            return ClassificacaoImc.BAIXO_PESO;
        }
        if (imc.compareTo(IMC_IDOSO_SOBREPESO) <= 0) {
            return ClassificacaoImc.EUTROFIA;
        }
        return ClassificacaoImc.SOBREPESO;
    }

    /**
     * RCQ = circunferência_cintura_cm / circunferência_quadril_cm.
     *
     * @return RCQ com 2 casas decimais, ou {@code null} se faltar qualquer medida
     */
    public BigDecimal calcularRelacaoCinturaQuadril(BigDecimal circCinturaCm, BigDecimal circQuadrilCm) {
        if (circCinturaCm == null || !ehPositivo(circQuadrilCm)) {
            return null;
        }
        return circCinturaCm.divide(circQuadrilCm, ESCALA, RoundingMode.HALF_UP);
    }

    /**
     * Risco elevado pela RCQ: ≥ 0,90 no masculino, ≥ 0,85 no feminino.
     *
     * @return {@code null} se a RCQ for desconhecida ou se o sexo não tiver
     *         ponto de corte definido — não se assume um padrão
     */
    public Boolean avaliarRcqElevada(BigDecimal relacaoCinturaQuadril, Sexo sexo) {
        if (relacaoCinturaQuadril == null || sexo == null) {
            return null;
        }
        return switch (sexo) {
            case MASCULINO -> relacaoCinturaQuadril.compareTo(RCQ_ELEVADA_MASCULINO) >= 0;
            case FEMININO -> relacaoCinturaQuadril.compareTo(RCQ_ELEVADA_FEMININO) >= 0;
            case OUTRO, NAO_INFORMADO -> null;
        };
    }

    /**
     * Risco metabólico pela circunferência da cintura isolada — a cintura
     * estratifica em três níveis, a RCQ só em dois.
     *
     * @return {@code null} se a cintura for desconhecida ou se o sexo não tiver
     *         ponto de corte definido
     */
    public RiscoCardiovascular avaliarRiscoCardiovascular(BigDecimal circCinturaCm, Sexo sexo) {
        if (circCinturaCm == null || sexo == null) {
            return null;
        }
        return switch (sexo) {
            case MASCULINO -> estratificar(circCinturaCm,
                    CINTURA_AUMENTADO_MASCULINO, CINTURA_MUITO_AUMENTADO_MASCULINO);
            case FEMININO -> estratificar(circCinturaCm,
                    CINTURA_AUMENTADO_FEMININO, CINTURA_MUITO_AUMENTADO_FEMININO);
            case OUTRO, NAO_INFORMADO -> null;
        };
    }

    private RiscoCardiovascular estratificar(BigDecimal circCinturaCm,
                                             BigDecimal corteAumentado,
                                             BigDecimal corteMuitoAumentado) {
        if (circCinturaCm.compareTo(corteAumentado) < 0) {
            return RiscoCardiovascular.BAIXO;
        }
        if (circCinturaCm.compareTo(corteMuitoAumentado) < 0) {
            return RiscoCardiovascular.AUMENTADO;
        }
        return RiscoCardiovascular.MUITO_AUMENTADO;
    }

    /** Divisor ausente ou não positivo é tratado como medida faltante, não como erro. */
    private boolean ehPositivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }
}
