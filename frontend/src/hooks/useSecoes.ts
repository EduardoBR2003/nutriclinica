import { useMutation, useQueryClient } from '@tanstack/react-query'

import { chaves } from '@/lib/chaves'
import { salvarSecao } from '@/services/secoes'
import type { SecaoProntuario } from '@/types/dominio'

/**
 * Um único hook serve as onze seções: o que muda entre elas é o verbo do
 * contrato (`PATCH` nas 1:1, `PUT` nas coleções) e o tipo do corpo.
 *
 * Invalida o detalhe do atendimento no sucesso porque `secoesPreenchidas` e
 * `editavel` são calculados pelo servidor — reproduzi-los no cliente seria
 * duplicar regra de domínio.
 */
export function useSalvarSecao<Req, Res = Req>(
  atendimentoId: number,
  secao: SecaoProntuario,
  metodo: 'patch' | 'put',
) {
  const queryClient = useQueryClient()
  return useMutation<Res, unknown, Req>({
    mutationFn: (corpo) => salvarSecao<Req, Res>(atendimentoId, secao, corpo, metodo),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: chaves.atendimentos.detalhe(atendimentoId),
      })
    },
  })
}
