package br.edu.nutriclinica.dto;

import br.edu.nutriclinica.domain.enums.ClassificacaoImc;
import br.edu.nutriclinica.domain.enums.RiscoCardiovascular;

import java.math.BigDecimal;

/**
 * Indicadores antropométricos derivados, calculados pelo backend.
 *
 * <p>Cada indicador é independente: qualquer um pode vir {@code null} quando
 * faltam as medidas de que depende, sem afetar os demais. Prontuário meio
 * preenchido é o estado normal durante a consulta.
 *
 * @param imc                   IMC em kg/m², 2 casas decimais
 * @param classificacaoImc      faixa do IMC conforme a idade; {@code null} para menores de 20 anos
 * @param relacaoCinturaQuadril RCQ, 2 casas decimais
 * @param rcqElevada            RCQ acima do corte para o sexo; campo derivado, não persistido
 * @param riscoCardiovascular   risco metabólico pela cintura isolada
 */
public record ResultadoAntropometrico(
        BigDecimal imc,
        ClassificacaoImc classificacaoImc,
        BigDecimal relacaoCinturaQuadril,
        Boolean rcqElevada,
        RiscoCardiovascular riscoCardiovascular
) {
}
