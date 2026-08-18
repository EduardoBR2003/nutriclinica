import { createContext } from 'react'

import type { Usuario } from '@/types/dominio'

export interface AuthContextValue {
  usuario: Usuario | null
  /** true enquanto a restauração da sessão ainda não terminou. */
  carregando: boolean
  login: (email: string, senha: string) => Promise<Usuario>
  logout: () => void
}

/** Em arquivo próprio para não quebrar o fast refresh do AuthProvider. */
export const AuthContext = createContext<AuthContextValue | null>(null)
