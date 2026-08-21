import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { chaves } from '@/lib/chaves'
import {
  adicionarComentario,
  listarComentarios,
  listarPendentes,
  obterAvaliacao,
  registrarAvaliacao,
} from '@/services/revisoes'
import type {
  Atendimento,
  Avaliacao,
  AvaliacaoRequest,
  ComentarioSecao,
  Pagina,
  SecaoProntuario,
} from '@/types/dominio'

export function useRevisoesPendentes(page: number, size: number) {
  return useQuery<Pagina<Atendimento>>({
    queryKey: chaves.revisoes.pendentes(page, size),
    queryFn: () => listarPendentes(page, size),
    placeholderData: keepPreviousData,
  })
}

export function useAvaliacao(atendimentoId: number | undefined, habilitado = true) {
  return useQuery<Avaliacao>({
    queryKey: chaves.revisoes.avaliacao(atendimentoId ?? 0),
    queryFn: () => obterAvaliacao(atendimentoId as number),
    enabled: atendimentoId !== undefined && habilitado,
    // Atendimento ainda não avaliado responde 404 — não é falha de rede.
    retry: false,
  })
}

export function useRegistrarAvaliacao(atendimentoId: number) {
  const queryClient = useQueryClient()
  return useMutation<Avaliacao, unknown, AvaliacaoRequest>({
    mutationFn: (corpo) => registrarAvaliacao(atendimentoId, corpo),
    onSuccess: (avaliacao) => {
      queryClient.setQueryData(chaves.revisoes.avaliacao(atendimentoId), avaliacao)
      // A avaliação conclui a revisão: muda o status e esvazia a fila.
      void queryClient.invalidateQueries({ queryKey: chaves.atendimentos.raiz })
      void queryClient.invalidateQueries({ queryKey: ['revisoes', 'pendentes'] })
    },
  })
}

export function useComentarios(atendimentoId: number | undefined) {
  return useQuery<ComentarioSecao[]>({
    queryKey: chaves.revisoes.comentarios(atendimentoId ?? 0),
    queryFn: () => listarComentarios(atendimentoId as number),
    enabled: atendimentoId !== undefined,
  })
}

export interface NovoComentario {
  secao: SecaoProntuario
  texto: string
}

export function useAdicionarComentario(atendimentoId: number) {
  const queryClient = useQueryClient()
  return useMutation<ComentarioSecao, unknown, NovoComentario>({
    mutationFn: (corpo) => adicionarComentario(atendimentoId, corpo),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: chaves.revisoes.comentarios(atendimentoId),
      })
    },
  })
}
