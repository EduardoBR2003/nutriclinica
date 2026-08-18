import { AxiosError } from 'axios'
import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'

import type { CampoErro, CodigoErro, Erro } from '@/types/dominio'

/**
 * Tradução do schema `Erro` do contrato para algo exibível.
 *
 * O contrato é explícito: o cliente distingue os erros pelo `codigo`, nunca
 * pelo texto de `mensagem`.
 */

export interface ErroApi {
  status?: number
  codigo?: CodigoErro | string
  mensagem?: string
  campos: CampoErro[]
}

function pareceErroDoContrato(corpo: unknown): corpo is Erro {
  return typeof corpo === 'object' && corpo !== null && ('codigo' in corpo || 'mensagem' in corpo)
}

/** Normaliza qualquer coisa lançada (AxiosError, erro de rede, throw solto). */
export function extrairErro(erro: unknown): ErroApi {
  if (erro instanceof AxiosError) {
    const corpo = erro.response?.data
    if (pareceErroDoContrato(corpo)) {
      return {
        status: erro.response?.status,
        codigo: corpo.codigo,
        mensagem: corpo.mensagem,
        campos: corpo.campos ?? [],
      }
    }
    return { status: erro.response?.status, mensagem: erro.message, campos: [] }
  }

  if (erro instanceof Error) {
    return { mensagem: erro.message, campos: [] }
  }

  return { campos: [] }
}

const MENSAGENS_PADRAO: Record<string, string> = {
  NAO_AUTORIZADO: 'Sessão inválida ou expirada. Entre novamente.',
  SEM_PERMISSAO: 'Você não tem permissão para esta ação.',
  NAO_ENCONTRADO: 'Registro não encontrado.',
  VALIDACAO: 'Verifique os campos destacados.',
  TRANSICAO_INVALIDA: 'A operação não é válida no estado atual do atendimento.',
  SEM_TERMO_CONSENTIMENTO: 'O paciente não possui termo de consentimento registrado.',
  SECOES_INCOMPLETAS: 'Faltam informações obrigatórias no prontuário.',
  ERRO_INTERNO: 'Erro inesperado no servidor. Tente novamente.',
}

/** Mensagem pronta para exibir ao usuário, com defaults em pt-BR. */
export function mensagemDeErro(erro: unknown): string {
  const { status, codigo, mensagem } = extrairErro(erro)

  if (mensagem) return mensagem
  if (codigo && MENSAGENS_PADRAO[codigo]) return MENSAGENS_PADRAO[codigo]
  if (status === undefined) {
    return 'Não foi possível falar com o servidor. Verifique sua conexão.'
  }
  return 'Não foi possível concluir a operação.'
}

/**
 * Joga o array `campos` do contrato nos erros de campo do React Hook Form.
 *
 * Aceita caminhos aninhados (`antropometria.pesoKg`, `diagnostico.etiologia`),
 * que o RHF resolve nativamente — é o formato que o 409 SECOES_INCOMPLETAS usa.
 * Devolve `true` se algum campo foi casado, para o chamador decidir se ainda
 * precisa mostrar uma mensagem geral.
 */
export function aplicarErrosDeCampo<T extends FieldValues>(
  erro: unknown,
  setError: UseFormSetError<T>,
  camposConhecidos?: readonly Path<T>[],
): boolean {
  const { campos } = extrairErro(erro)
  let algumCasou = false

  for (const item of campos) {
    if (!item.campo) continue
    const caminho = item.campo as Path<T>
    if (camposConhecidos && !camposConhecidos.includes(caminho)) continue

    setError(caminho, { type: 'server', message: item.mensagem })
    algumCasou = true
  }

  return algumCasou
}
