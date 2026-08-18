import { useContext } from 'react'

import { AuthContext, type AuthContextValue } from '@/lib/authContext'

export function useAuth(): AuthContextValue {
  const contexto = useContext(AuthContext)
  if (!contexto) {
    throw new Error('useAuth precisa estar dentro de <AuthProvider>.')
  }
  return contexto
}
