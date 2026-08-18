import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { useNavigate } from 'react-router'

import { api } from '@/lib/api'
import { AuthContext, type AuthContextValue } from '@/lib/authContext'
import { queryClient } from '@/lib/queryClient'
import {
  definirSessao,
  getRefreshToken,
  limparSessao,
  registrarAoExpirar,
} from '@/lib/sessao'
import type { TokenResponse, Usuario } from '@/types/dominio'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(null)
  const [carregando, setCarregando] = useState(true)
  const navigate = useNavigate()

  // Expiração definitiva vinda do interceptor: zera o usuário e volta ao login
  // sem recarregar a página.
  useEffect(() => {
    return registrarAoExpirar(() => {
      setUsuario(null)
      queryClient.clear()
      navigate('/login', { replace: true })
    })
  }, [navigate])

  // Restauração da sessão ao montar. O access token vive só em memória, então
  // depois de um reload este /me toma 401 e o próprio interceptor renova e
  // repete — a restauração reusa o caminho de refresh, sem código duplicado.
  useEffect(() => {
    let cancelado = false

    if (!getRefreshToken()) {
      setCarregando(false)
      return
    }

    api
      .get<Usuario>('/api/auth/me')
      .then(({ data }) => {
        if (!cancelado) setUsuario(data)
      })
      .catch(() => {
        if (!cancelado) {
          limparSessao()
          setUsuario(null)
        }
      })
      .finally(() => {
        if (!cancelado) setCarregando(false)
      })

    return () => {
      cancelado = true
    }
  }, [])

  const login = useCallback(async (email: string, senha: string) => {
    const { data } = await api.post<TokenResponse>('/api/auth/login', { email, senha })
    definirSessao(data)
    setUsuario(data.usuario)
    return data.usuario
  }, [])

  const logout = useCallback(() => {
    limparSessao()
    setUsuario(null)
    // Dado de saúde não pode vazar de uma sessão para a próxima.
    queryClient.clear()
    navigate('/login', { replace: true })
  }, [navigate])

  const valor = useMemo<AuthContextValue>(
    () => ({ usuario, carregando, login, logout }),
    [usuario, carregando, login, logout],
  )

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}
