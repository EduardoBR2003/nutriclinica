package br.edu.nutriclinica.dto;

import java.util.List;

/**
 * Corpo do `PUT /api/usuarios/{id}/vinculos`.
 *
 * <p>Lista ausente é tratada como lista vazia — "este supervisor não orienta
 * ninguém" é um estado legítimo, e o cliente que desmarcou todos manda
 * exatamente isso.
 */
public record VinculosRequest(List<Long> estagiarioIds) {

    public List<Long> idsOuVazio() {
        return estagiarioIds == null ? List.of() : estagiarioIds;
    }
}
