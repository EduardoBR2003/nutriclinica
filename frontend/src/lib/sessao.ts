import type { TokenResponse } from '@/types/dominio'

/**
 * Guarda dos tokens da sessão, sem nenhuma dependência de React.
 *
 * Vive separado do AuthContext de propósito: o interceptor de `api.ts` precisa
 * ler e trocar os tokens, e não pode importar o contexto sem criar o ciclo
 * auth -> api -> auth. Aqui a comunicação de volta para a camada React é feita
 * por assinatura (`registrarAoExpirar`).
 *
 * O access token fica só em memória — some no reload, e é justamente o refresh
 * token do localStorage que restaura a sessão.
 */

const CHAVE_REFRESH = 'nutriclinica.refreshToken'

let accessToken: string | null = null

export function getAccessToken(): string | null {
  return accessToken
}

export function getRefreshToken(): string | null {
  try {
    return localStorage.getItem(CHAVE_REFRESH)
  } catch {
    return null
  }
}

export function definirSessao(tokens: TokenResponse): void {
  accessToken = tokens.accessToken
  try {
    localStorage.setItem(CHAVE_REFRESH, tokens.refreshToken)
  } catch {
    // localStorage indisponível (modo privado, por exemplo): a sessão ainda
    // funciona até o reload, só não sobrevive a ele.
  }
}

export function limparSessao(): void {
  accessToken = null
  try {
    localStorage.removeItem(CHAVE_REFRESH)
  } catch {
    // nada a fazer
  }
}

let aoExpirar: (() => void) | null = null

/** O AuthProvider assina para reagir à expiração. Devolve a função de cancelamento. */
export function registrarAoExpirar(callback: () => void): () => void {
  aoExpirar = callback
  return () => {
    if (aoExpirar === callback) aoExpirar = null
  }
}

/**
 * Chamado quando a renovação falhou de vez: derruba a sessão e avisa a app.
 * Sem assinante (ainda fora da árvore React), cai na navegação do navegador.
 */
export function notificarExpiracao(): void {
  limparSessao()
  if (aoExpirar) {
    aoExpirar()
  } else if (typeof window !== 'undefined') {
    window.location.assign('/login')
  }
}
