package br.edu.nutriclinica.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Schema `AntropometriaRequest` do contrato — apenas os valores <b>medidos</b>.
 *
 * <p>Indicadores derivados (imc, classificacaoImc, relacaoCinturaQuadril,
 * rcqElevada, riscoCardiovascular) não têm campo aqui de propósito: é assim que
 * um valor calculado enviado pelo cliente é descartado antes de qualquer
 * chance de ser persistido. O {@code ignoreUnknown} deixa esse descarte
 * explícito em vez de depender do padrão do Jackson.
 *
 * <p>Todos os campos são opcionais: prontuário meio preenchido é o estado
 * normal durante a consulta. As faixas validadas abaixo são as mesmas de
 * {@code ck_antro_peso} e {@code ck_antro_altura}, para que a violação vire
 * 422 no schema `Erro` em vez de erro de constraint do banco.
 *
 * <p>O percentual de gordura e a massa magra não são validados aqui: por
 * {@code regras-calculo.md} §5, divergência nesses dois gera aviso, não erro
 * fatal, e derrubar o salvamento da seção seria o comportamento errado.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AntropometriaRequest(

        @DecimalMin(value = "1", message = "O peso deve ser de no mínimo 1 kg.")
        @DecimalMax(value = "400", message = "O peso deve ser de no máximo 400 kg.")
        BigDecimal pesoKg,

        @DecimalMin(value = "30", message = "A altura deve ser de no mínimo 30 cm.")
        @DecimalMax(value = "250", message = "A altura deve ser de no máximo 250 cm.")
        BigDecimal alturaCm,

        @DecimalMin(value = "0", inclusive = false, message = "A circunferência da cintura deve ser positiva.")
        BigDecimal circCinturaCm,

        @DecimalMin(value = "0", inclusive = false, message = "A circunferência do quadril deve ser positiva.")
        BigDecimal circQuadrilCm,

        BigDecimal percentualGordura,

        BigDecimal massaMagraKg,

        LocalDate aferidoEm) {
}
