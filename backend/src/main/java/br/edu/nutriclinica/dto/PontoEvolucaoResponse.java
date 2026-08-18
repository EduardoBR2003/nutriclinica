package br.edu.nutriclinica.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Schema `PontoEvolucao` do contrato: um ponto da série histórica que o
 * frontend desenha no gráfico de evolução.
 *
 * <p>Montado direto pela query, sem carregar as entidades: a série só precisa
 * de cinco colunas de cada atendimento aprovado.
 */
public record PontoEvolucaoResponse(
        LocalDate dataConsulta,
        BigDecimal pesoKg,
        BigDecimal imc,
        BigDecimal circCinturaCm,
        BigDecimal circQuadrilCm) {
}
