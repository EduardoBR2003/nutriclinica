import { useMutation } from '@tanstack/react-query'

import { useAuth } from '@/hooks/useAuth'
import type { Usuario } from '@/types/dominio'

export interface CredenciaisLogin {
  email: string
  senha: string
}

/**
 * Autenticação como mutation do TanStack Query — entrega `isPending` e `error`
 * ao formulário e mantém a convenção de que toda chamada de API passa por um
 * hook do Query.
 */
export function useLogin() {
  const { login } = useAuth()

  return useMutation<Usuario, unknown, CredenciaisLogin>({
    mutationFn: ({ email, senha }) => login(email, senha),
  })
}
