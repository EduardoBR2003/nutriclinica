package br.edu.nutriclinica.domain.enums;

/**
 * Classificação do estado nutricional pelo IMC.
 *
 * <p>Adultos de 20 a 59 anos usam os pontos de corte da OMS (Technical Report
 * Series 894, 2000); a partir de 60 anos usa-se Lipschitz (1994), adotado pelo
 * SISVAN, que só distingue BAIXO_PESO, EUTROFIA e SOBREPESO.
 *
 * <p>Menores de 20 anos não são classificados pelo IMC absoluto — a avaliação
 * depende do escore-z das curvas da OMS, fora do escopo desta versão.
 */
public enum ClassificacaoImc {
    BAIXO_PESO,
    EUTROFIA,
    SOBREPESO,
    OBESIDADE_GRAU_I,
    OBESIDADE_GRAU_II,
    OBESIDADE_GRAU_III
}
