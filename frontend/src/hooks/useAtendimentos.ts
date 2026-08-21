import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { chaves, type FiltroAtendimentos } from '@/lib/chaves'
import {
  criarAtendimento,
  listarAtendimentos,
  obterAtendimento,
  submeterAtendimento,
} from '@/services/atendimentos'
import type {
  Atendimento,
  AtendimentoCompleto,
  NovoAtendimento,
  Pagina,
} from '@/types/dominio'

export function useAtendimentos(filtro: FiltroAtendimentos) {
  return useQuery<Pagina<Atendimento>>({
    queryKey: chaves.atendimentos.lista(filtro),
    queryFn: () => listarAtendimentos(filtro),
    placeholderData: keepPreviousData,
  })
}

export function useAtendimento(id: number | undefined) {
  return useQuery<AtendimentoCompleto>({
    queryKey: chaves.atendimentos.detalhe(id ?? 0),
    queryFn: () => obterAtendimento(id as number),
    enabled: id !== undefined,
  })
}

export function useCriarAtendimento() {
  const queryClient = useQueryClient()
  return useMutation<Atendimento, unknown, NovoAtendimento>({
    mutationFn: criarAtendimento,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: chaves.atendimentos.raiz })
    },
  })
}

export function useSubmeterAtendimento(id: number) {
  const queryClient = useQueryClient()
  return useMutation<Atendimento, unknown, void>({
    mutationFn: () => submeterAtendimento(id),
    onSuccess: () => {
      // A transição muda status, `editavel` e a fila do supervisor.
      void queryClient.invalidateQueries({ queryKey: chaves.atendimentos.raiz })
      void queryClient.invalidateQueries({ queryKey: chaves.revisoes.raiz })
    },
  })
}
