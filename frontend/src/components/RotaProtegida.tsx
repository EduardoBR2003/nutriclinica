import { Navigate, Outlet, useLocation } from 'react-router'

import { useAuth } from '@/hooks/useAuth'
import type { Perfil } from '@/types/dominio'

interface RotaProtegidaProps {
  /** Quando omitido, basta estar autenticado. */
  perfis?: readonly Perfil[]
}

/**
 * Rota de layout que guarda tudo o que estiver aninhado nela.
 * Sem sessão vai para /login; com perfil fora da lista, para /sem-permissao —
 * negar acesso não é o mesmo que não estar autenticado.
 */
export function RotaProtegida({ perfis }: RotaProtegidaProps) {
  const { usuario, carregando } = useAuth()
  const location = useLocation()

  if (carregando) {
    return (
      <div className="flex min-h-svh items-center justify-center">
        <p className="text-muted-foreground text-sm">Carregando…</p>
      </div>
    )
  }

  if (!usuario) {
    // Guarda a origem para devolver o usuário ao destino depois do login.
    return <Navigate to="/login" replace state={{ de: location.pathname }} />
  }

  if (perfis && !perfis.includes(usuario.perfil)) {
    return <Navigate to="/sem-permissao" replace />
  }

  return <Outlet />
}
