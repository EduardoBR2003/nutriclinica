import { Navigate } from 'react-router'

import { useAuth } from '@/hooks/useAuth'
import type { Perfil } from '@/types/dominio'

/** Destino inicial de cada perfil ao entrar em `/`. */
const DESTINO: Record<Perfil, string> = {
  ESTAGIARIO: '/atendimentos',
  SUPERVISOR: '/revisoes',
  ADMIN: '/admin/usuarios',
}

export default function Inicio() {
  const { usuario } = useAuth()
  if (!usuario) return <Navigate to="/login" replace />

  return <Navigate to={DESTINO[usuario.perfil]} replace />
}
