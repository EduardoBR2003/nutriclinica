import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { chaves, type FiltroPacientes } from '@/lib/chaves'
import {
  atualizarPaciente,
  criarPaciente,
  listarPacientes,
  obterEvolucao,
  obterPaciente,
  obterTermo,
  salvarTermo,
} from '@/services/pacientes'
import type {
  Pagina,
  Paciente,
  PacienteRequest,
  PontoEvolucao,
  TermoConsentimento,
  TermoConsentimentoRequest,
} from '@/types/dominio'

export function usePacientes(filtro: FiltroPacientes) {
  return useQuery<Pagina<Paciente>>({
    queryKey: chaves.pacientes.lista(filtro),
    queryFn: () => listarPacientes(filtro),
    // Sem isto a tabela pisca a cada página trocada.
    placeholderData: keepPreviousData,
  })
}

export function usePaciente(id: number | undefined) {
  return useQuery<Paciente>({
    queryKey: chaves.pacientes.detalhe(id ?? 0),
    queryFn: () => obterPaciente(id as number),
    enabled: id !== undefined,
  })
}

export interface SalvarPacienteInput {
  id?: number
  corpo: PacienteRequest
}

export function useSalvarPaciente() {
  const queryClient = useQueryClient()
  return useMutation<Paciente, unknown, SalvarPacienteInput>({
    mutationFn: ({ id, corpo }) =>
      id === undefined ? criarPaciente(corpo) : atualizarPaciente(id, corpo),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: chaves.pacientes.raiz })
    },
  })
}

export function useTermo(id: number | undefined) {
  return useQuery<TermoConsentimento>({
    queryKey: chaves.pacientes.termo(id ?? 0),
    queryFn: () => obterTermo(id as number),
    enabled: id !== undefined,
    // Paciente sem termo responde 404 — é resposta esperada, não vale repetir.
    retry: false,
  })
}

export function useSalvarTermo(id: number) {
  const queryClient = useQueryClient()
  return useMutation<TermoConsentimento, unknown, TermoConsentimentoRequest>({
    mutationFn: (corpo) => salvarTermo(id, corpo),
    onSuccess: (termo) => {
      queryClient.setQueryData(chaves.pacientes.termo(id), termo)
      // `possuiTermo` muda na listagem e destrava a abertura de atendimento.
      void queryClient.invalidateQueries({ queryKey: chaves.pacientes.raiz })
    },
  })
}

export function useEvolucao(id: number | undefined) {
  return useQuery<PontoEvolucao[]>({
    queryKey: chaves.pacientes.evolucao(id ?? 0),
    queryFn: () => obterEvolucao(id as number),
    enabled: id !== undefined,
  })
}
