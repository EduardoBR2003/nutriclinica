package br.edu.nutriclinica.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.openapitools.jackson.nullable.JsonNullable;

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
 * <p>Cada campo é {@code JsonNullable} porque o PATCH é parcial: a chave que não
 * veio mantém a medida gravada, a chave enviada como {@code null} apaga. A
 * balança e a fita métrica entram na tela em momentos diferentes da consulta, e
 * salvar o peso não pode zerar a cintura anotada cinco minutos antes.
 *
 * <p>Todos os campos são opcionais: prontuário meio preenchido é o estado
 * normal durante a consulta. As faixas validadas abaixo são as mesmas de
 * {@code ck_antro_peso} e {@code ck_antro_altura}, para que a violação vire
 * 422 no schema `Erro` em vez de erro de constraint do banco. Elas ficam no
 * elemento do container, não no campo, porque é o valor dentro do
 * {@code JsonNullable} que precisa estar na faixa.
 *
 * <p>O percentual de gordura e a massa magra não são validados aqui: por
 * {@code regras-calculo.md} §5, divergência nesses dois gera aviso, não erro
 * fatal, e derrubar o salvamento da seção seria o comportamento errado.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AntropometriaRequest(

        JsonNullable<@DecimalMin(value = "1", message = "O peso deve ser de no mínimo 1 kg.")
                     @DecimalMax(value = "400", message = "O peso deve ser de no máximo 400 kg.")
                     BigDecimal> pesoKg,

        JsonNullable<@DecimalMin(value = "30", message = "A altura deve ser de no mínimo 30 cm.")
                     @DecimalMax(value = "250", message = "A altura deve ser de no máximo 250 cm.")
                     BigDecimal> alturaCm,

        JsonNullable<@DecimalMin(value = "0", inclusive = false,
                                 message = "A circunferência da cintura deve ser positiva.")
                     BigDecimal> circCinturaCm,

        JsonNullable<@DecimalMin(value = "0", inclusive = false,
                                 message = "A circunferência do quadril deve ser positiva.")
                     BigDecimal> circQuadrilCm,

        JsonNullable<BigDecimal> percentualGordura,

        JsonNullable<BigDecimal> massaMagraKg,

        JsonNullable<LocalDate> aferidoEm) {
}
