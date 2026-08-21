import { api } from '@/lib/api'
import type { FiltroUsuarios } from '@/lib/chaves'
import type { CadastroRequest, Pagina, Usuario, UsuarioRequest } from '@/types/dominio'

/**
 * `/api/usuarios` é exclusivo do ADMIN, com uma exceção: `/supervisores`, que é
 * a lista que o estagiário usa para escolher quem vai revisar o atendimento.
 */

export async function listarUsuarios(filtro: FiltroUsuarios) {
  const { data } = await api.get<Pagina<Usuario>>('/api/usuarios', {
    params: { perfil: filtro.perfil, page: filtro.page, size: filtro.size },
  })
  return data
}

export async function obterUsuario(id: number) {
  const { data } = await api.get<Usuario>(`/api/usuarios/${id}`)
  return data
}

export async function criarUsuario(corpo: UsuarioRequest) {
  const { data } = await api.post<Usuario>('/api/usuarios', corpo)
  return data
}

export async function atualizarUsuario(id: number, corpo: UsuarioRequest) {
  const { data } = await api.put<Usuario>(`/api/usuarios/${id}`, corpo)
  return data
}

/** Supervisores que orientam o estagiário autenticado. */
export async function listarSupervisoresDisponiveis() {
  const { data } = await api.get<Usuario[]>('/api/usuarios/supervisores')
  return data
}

export async function listarVinculos(supervisorId: number) {
  const { data } = await api.get<Usuario[]>(`/api/usuarios/${supervisorId}/vinculos`)
  return data
}

export async function salvarVinculos(supervisorId: number, estagiarioIds: number[]) {
  await api.put(`/api/usuarios/${supervisorId}/vinculos`, { estagiarioIds })
}

/** Auto-cadastro da tela de entrada: rota pública, conta nasce inativa. */
export async function cadastrar(corpo: CadastroRequest) {
  const { data } = await api.post<Usuario>('/api/auth/cadastro', corpo)
  return data
}
