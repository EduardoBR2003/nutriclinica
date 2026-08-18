package br.edu.nutriclinica.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Corpo de erro padrão da API. Espelha o schema `Erro` do docs/api.yaml.
 * O array `campos` só é preenchido em falhas de validação; nos demais casos
 * fica nulo e é omitido do JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(
        String codigo,
        String mensagem,
        OffsetDateTime timestamp,
        List<CampoErro> campos) {

    public record CampoErro(String campo, String mensagem) {
    }

    public static ErroResponse de(String codigo, String mensagem) {
        return new ErroResponse(codigo, mensagem, OffsetDateTime.now(), null);
    }

    public static ErroResponse de(String codigo, String mensagem, List<CampoErro> campos) {
        return new ErroResponse(codigo, mensagem, OffsetDateTime.now(), campos);
    }
}
