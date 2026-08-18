package br.edu.nutriclinica.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Corpo de POST /api/atendimentos.
 *
 * <p>Não recebe {@code estagiarioId}: o estagiário é sempre o usuário
 * autenticado. Aceitá-lo do cliente permitiria abrir atendimento em nome de
 * outra pessoa.
 *
 * <p>Também não recebe {@code numeroProntuario} nem {@code status}: o número é
 * gerado pelo contador sob lock e o status inicial é sempre RASCUNHO.
 */
public record AtendimentoRequest(

        @NotNull(message = "O paciente é obrigatório.")
        Long pacienteId,

        @NotNull(message = "O supervisor é obrigatório.")
        Long supervisorId,

        @NotNull(message = "A data da consulta é obrigatória.")
        LocalDate dataConsulta) {
}
