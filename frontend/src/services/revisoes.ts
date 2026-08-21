import { api } from '@/lib/api'
import type {
  Atendimento,
  Avaliacao,
  AvaliacaoRequest,
  ComentarioSecao,
  Pagina,
  SecaoProntuario,
} from '@/types/dominio'

/** Fila do supervisor designado, avaliação por rubrica e comentários por seção. */

export async function listarPendentes(page: number, size: number) {
  const { data } = await api.get<Pagina<Atendimento>>('/api/revisoes/pendentes', {
    params: { page, size },
  })
  return data
}

export async function obterAvaliacao(atendimentoId: number) {
  const { data } = await api.get<Avaliacao>(`/api/atendimentos/${atendimentoId}/avaliacao`)
  return data
}

export async function registrarAvaliacao(atendimentoId: number, corpo: AvaliacaoRequest) {
  const { data } = await api.post<Avaliacao>(
    `/api/atendimentos/${atendimentoId}/avaliacao`,
    corpo,
  )
  return data
}

export async function listarComentarios(atendimentoId: number) {
  const { data } = await api.get<ComentarioSecao[]>(
    `/api/atendimentos/${atendimentoId}/comentarios`,
  )
  return data
}

export async function adicionarComentario(
  atendimentoId: number,
  corpo: { secao: SecaoProntuario; texto: string },
) {
  const { data } = await api.post<ComentarioSecao>(
    `/api/atendimentos/${atendimentoId}/comentarios`,
    corpo,
  )
  return data
}
