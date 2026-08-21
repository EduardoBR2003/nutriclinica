import { api } from '@/lib/api'
import type { FiltroAtendimentos } from '@/lib/chaves'
import type {
  Atendimento,
  AtendimentoCompleto,
  NovoAtendimento,
  Pagina,
} from '@/types/dominio'

/** Chamadas de `/api/atendimentos`. O escopo por perfil é resolvido no servidor. */

export async function listarAtendimentos(filtro: FiltroAtendimentos) {
  const { data } = await api.get<Pagina<Atendimento>>('/api/atendimentos', {
    params: {
      status: filtro.status,
      pacienteId: filtro.pacienteId,
      page: filtro.page,
      size: filtro.size,
    },
  })
  return data
}

export async function obterAtendimento(id: number) {
  const { data } = await api.get<AtendimentoCompleto>(`/api/atendimentos/${id}`)
  return data
}

export async function criarAtendimento(corpo: NovoAtendimento) {
  const { data } = await api.post<Atendimento>('/api/atendimentos', corpo)
  return data
}

export async function submeterAtendimento(id: number) {
  const { data } = await api.post<Atendimento>(`/api/atendimentos/${id}/submeter`)
  return data
}
