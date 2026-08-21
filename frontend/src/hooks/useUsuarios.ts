import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { chaves, type FiltroUsuarios } from '@/lib/chaves'
import {
  atualizarUsuario,
  cadastrar,
  criarUsuario,
  listarSupervisoresDisponiveis,
  listarUsuarios,
  listarVinculos,
  obterUsuario,
  salvarVinculos,
} from '@/services/usuarios'
import type {
  CadastroRequest,
  Pagina,
  Perfil,
  Usuario,
  UsuarioRequest,
} from '@/types/dominio'

export function useUsuarios(filtro: FiltroUsuarios) {
  return useQuery<Pagina<Usuario>>({
    queryKey: chaves.usuarios.lista(filtro),
    queryFn: () => listarUsuarios(filtro),
    placeholderData: keepPreviousData,
  })
}

/** Lista completa de um perfil, para os selects e checkboxes do admin. */
export function useUsuariosPorPerfil(perfil: Perfil) {
  return useUsuarios({ perfil, page: 0, size: 100 })
}

export function useUsuario(id: number | undefined) {
  return useQuery<Usuario>({
    queryKey: chaves.usuarios.detalhe(id ?? 0),
    queryFn: () => obterUsuario(id as number),
    enabled: id !== undefined,
  })
}

/**
 * Supervisores que o estagiário autenticado pode escolher ao abrir atendimento.
 *
 * <p>Não é `useUsuariosPorPerfil('SUPERVISOR')`: aquela rota é do ADMIN e lista
 * a casa inteira. Esta traz só quem tem vínculo com quem pediu — o mesmo
 * conjunto que o `POST /api/atendimentos` aceita, então o select nunca oferece
 * uma opção que vai voltar como 422.
 */
export function useSupervisoresDisponiveis() {
  return useQuery<Usuario[]>({
    queryKey: chaves.usuarios.supervisores,
    queryFn: listarSupervisoresDisponiveis,
  })
}

/** Estagiários hoje orientados por um supervisor — abre a tela de vínculos marcada. */
export function useVinculos(supervisorId: number | undefined) {
  return useQuery<Usuario[]>({
    queryKey: chaves.usuarios.vinculos(supervisorId ?? 0),
    queryFn: () => listarVinculos(supervisorId as number),
    enabled: supervisorId !== undefined,
  })
}

export interface SalvarUsuarioInput {
  id?: number
  corpo: UsuarioRequest
}

export function useSalvarUsuario() {
  const queryClient = useQueryClient()
  return useMutation<Usuario, unknown, SalvarUsuarioInput>({
    mutationFn: ({ id, corpo }) =>
      id === undefined ? criarUsuario(corpo) : atualizarUsuario(id, corpo),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: chaves.usuarios.raiz })
    },
  })
}

export interface VinculosInput {
  supervisorId: number
  estagiarioIds: number[]
}

export function useSalvarVinculos() {
  const queryClient = useQueryClient()
  return useMutation<void, unknown, VinculosInput>({
    mutationFn: ({ supervisorId, estagiarioIds }) => salvarVinculos(supervisorId, estagiarioIds),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: chaves.usuarios.raiz })
    },
  })
}

/**
 * Auto-cadastro da tela de entrada. Não abre sessão de propósito: a conta nasce
 * inativa e só entra depois que um administrador a libera.
 */
export function useCadastro() {
  return useMutation<Usuario, unknown, CadastroRequest>({
    mutationFn: cadastrar,
  })
}
