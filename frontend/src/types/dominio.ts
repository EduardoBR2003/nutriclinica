import type { components } from '@/types/api'

/**
 * Aliases de domínio sobre os tipos gerados de `docs/api.yaml`.
 *
 * O contrato não declara `required` nos schemas de resposta, então tudo em
 * `api.d.ts` sai opcional (`usuario.perfil?: Perfil`) — inviável para lógica de
 * perfil sob `strict`. Aqui os tipos que a app trata como sempre presentes são
 * estreitados num lugar só. A correção de raiz é acrescentar `required:` ao
 * contrato, o que fica para um bloco futuro por tocar a fonte da verdade.
 */

export type Perfil = components['schemas']['Perfil']
export type StatusAtendimento = components['schemas']['StatusAtendimento']
export type SecaoProntuario = components['schemas']['SecaoProntuario']

export type Usuario = Required<components['schemas']['Usuario']>
export type TokenResponse = Required<
  Omit<components['schemas']['TokenResponse'], 'usuario'>
> & { usuario: Usuario }

export type Erro = components['schemas']['Erro']
export type CampoErro = NonNullable<Erro['campos']>[number]

/** Códigos estáveis emitidos pelo backend — é por eles que o cliente distingue os erros. */
export const CODIGOS_ERRO = [
  'VALIDACAO',
  'NAO_AUTORIZADO',
  'SEM_PERMISSAO',
  'NAO_ENCONTRADO',
  'TRANSICAO_INVALIDA',
  'SEM_TERMO_CONSENTIMENTO',
  'SECOES_INCOMPLETAS',
  'ERRO_INTERNO',
] as const

export type CodigoErro = (typeof CODIGOS_ERRO)[number]
