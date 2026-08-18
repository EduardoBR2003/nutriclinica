import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'

import {
  definirSessao,
  getAccessToken,
  getRefreshToken,
  notificarExpiracao,
} from '@/lib/sessao'
import type { TokenResponse } from '@/types/dominio'

declare module 'axios' {
  export interface InternalAxiosRequestConfig {
    /** Marca que a requisição já foi repetida uma vez após renovar o token. */
    _retentado?: boolean
  }
}

export const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

const ROTA_LOGIN = '/api/auth/login'
const ROTA_REFRESH = '/api/auth/refresh'

/**
 * Cliente HTTP único da aplicação. Todo acesso à API passa por aqui,
 * sempre embrulhado em um hook do TanStack Query — nunca chamado
 * diretamente de dentro de um componente.
 */
export const api = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  // O backend usa Authorization: Bearer e responde com allowCredentials(false);
  // mandar cookie aqui só quebraria o CORS.
  withCredentials: false,
})

api.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token && !config.headers.Authorization) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * A "fila" de renovação: uma única promise em escopo de módulo.
 *
 * Quando várias requisições tomam 401 juntas, todas aguardam *esta mesma*
 * promise, então sai um único POST /api/auth/refresh. Isso não é otimização:
 * o backend rotaciona o refresh token (revoga o apresentado e emite outro), de
 * modo que dois refresh em paralelo invalidariam um ao outro e derrubariam a
 * sessão.
 */
let refreshEmAndamento: Promise<string> | null = null

function renovarAccessToken(): Promise<string> {
  if (refreshEmAndamento) return refreshEmAndamento

  const refreshToken = getRefreshToken()
  if (!refreshToken) return Promise.reject(new Error('Sessão sem refresh token.'))

  refreshEmAndamento = axios
    // Instância crua de propósito: passar por `api` recursaria nos interceptors.
    .post<TokenResponse>(
      `${BASE_URL}${ROTA_REFRESH}`,
      { refreshToken },
      { headers: { 'Content-Type': 'application/json' } },
    )
    .then(({ data }) => {
      definirSessao(data)
      return data.accessToken
    })
    .finally(() => {
      refreshEmAndamento = null
    })

  return refreshEmAndamento
}

function ehRotaDeAutenticacao(url: string | undefined): boolean {
  if (!url) return false
  return url.includes(ROTA_LOGIN) || url.includes(ROTA_REFRESH)
}

api.interceptors.response.use(
  (resposta) => resposta,
  async (erro: AxiosError) => {
    const original = erro.config as InternalAxiosRequestConfig | undefined

    if (!original || erro.response?.status !== 401) {
      return Promise.reject(erro)
    }

    // Um 401 de login ou de refresh é resposta legítima, não gatilho de renovação.
    if (ehRotaDeAutenticacao(original.url)) {
      return Promise.reject(erro)
    }

    // Já renovamos uma vez e tomamos 401 de novo: a sessão acabou mesmo.
    if (original._retentado) {
      notificarExpiracao()
      return Promise.reject(erro)
    }

    original._retentado = true

    try {
      const novoToken = await renovarAccessToken()
      original.headers.Authorization = `Bearer ${novoToken}`
      return await api(original)
    } catch {
      notificarExpiracao()
      return Promise.reject(erro)
    }
  },
)
