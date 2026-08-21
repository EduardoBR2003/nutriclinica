import { api } from '@/lib/api'
import { ROTA_SECAO } from '@/lib/chaves'
import type { SecaoProntuario } from '@/types/dominio'

/**
 * Salvamento por seção, como manda o contrato: `PATCH` parcial nas sete seções
 * 1:1 e `PUT` que substitui a coleção inteira nas quatro de lista.
 */

export async function salvarSecao<Req, Res = Req>(
  atendimentoId: number,
  secao: SecaoProntuario,
  corpo: Req,
  metodo: 'patch' | 'put',
): Promise<Res> {
  const url = `/api/atendimentos/${atendimentoId}/${ROTA_SECAO[secao]}`
  const { data } =
    metodo === 'patch' ? await api.patch<Res>(url, corpo) : await api.put<Res>(url, corpo)
  return data
}
