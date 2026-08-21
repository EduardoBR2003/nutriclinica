import { NavLink } from 'react-router'

import { Avatar } from '@/components/Avatar'
import { useAuth } from '@/hooks/useAuth'
import { useRevisoesPendentes } from '@/hooks/useRevisoes'
import { PERFIL } from '@/lib/rotulos'
import { cn } from '@/lib/utils'
import type { Perfil } from '@/types/dominio'

interface ItemNav {
  para: string
  rotulo: string
  /** Rotas filhas que também acendem o item. */
  prefixos?: readonly string[]
}

const NAV: Record<Perfil, readonly ItemNav[]> = {
  ESTAGIARIO: [
    { para: '/atendimentos', rotulo: 'Atendimentos' },
    { para: '/pacientes', rotulo: 'Pacientes' },
  ],
  SUPERVISOR: [
    { para: '/revisoes', rotulo: 'Fila de revisão', prefixos: ['/atendimentos'] },
    { para: '/pacientes', rotulo: 'Pacientes' },
  ],
  ADMIN: [
    { para: '/admin/usuarios', rotulo: 'Usuários' },
    { para: '/admin/vinculos', rotulo: 'Vínculos' },
  ],
}

const SUBTITULO: Record<Perfil, string> = {
  ESTAGIARIO: 'Estágio',
  SUPERVISOR: 'Supervisão',
  ADMIN: 'Administração',
}

export function SidebarApp() {
  const { usuario, logout } = useAuth()
  const perfil = usuario?.perfil ?? 'ESTAGIARIO'

  // A fila só existe para o supervisor designado; para os outros nem consulta.
  const pendentes = useRevisoesPendentes(0, 1)
  const fila = perfil === 'SUPERVISOR' ? (pendentes.data?.totalElements ?? 0) : 0

  if (!usuario) return null

  return (
    <aside className="bg-surface border-line-card sticky top-0 flex h-svh flex-col gap-[22px] self-start border-r px-4 py-[22px]">
      <div className="flex items-center gap-2.5 px-2">
        <span className="bg-brand inline-flex size-[34px] items-center justify-center rounded-[11px] text-2xl font-bold text-white">
          N
        </span>
        <span className="flex flex-col leading-tight">
          <span className="text-xl font-bold tracking-snug">NutriClinica</span>
          <span className="text-ink-muted text-xs">{SUBTITULO[perfil]}</span>
        </span>
      </div>

      <nav className="flex flex-col gap-0.5">
        {NAV[perfil].map((item) => (
          <NavLink
            key={item.para}
            to={item.para}
            className={({ isActive }) =>
              cn(
                'flex h-10 items-center gap-2.5 rounded-lg px-3 text-lg',
                isActive
                  ? 'bg-brand-tint text-brand-text font-semibold'
                  : 'text-ink-soft hover:bg-surface-alt font-medium',
              )
            }
          >
            <span className="min-w-0 flex-1 text-left">{item.rotulo}</span>
            {item.para === '/revisoes' && fila > 0 ? (
              <span className="bg-cta inline-flex h-5 min-w-5 items-center justify-center rounded-full px-1.5 text-xs font-bold text-white">
                {fila}
              </span>
            ) : null}
          </NavLink>
        ))}
      </nav>

      <div className="mt-auto flex flex-col gap-2.5">
        <div className="bg-surface-alt border-line-card flex items-center gap-2.5 rounded-2xl border p-3">
          <Avatar nome={usuario.nome} tamanho="sm" className="size-[34px] rounded-[11px]" />
          <span className="min-w-0 flex-1">
            <span className="block truncate text-base leading-tight font-semibold">
              {usuario.nome}
            </span>
            <span className="text-ink-muted mt-px block text-xs">{PERFIL[usuario.perfil]}</span>
          </span>
        </div>
        <button
          type="button"
          onClick={logout}
          className="text-ink-soft bg-surface border-line-card h-9 rounded-[11px] border text-base font-semibold hover:bg-[oklch(0.965_0.004_262)]"
        >
          Sair
        </button>
      </div>
    </aside>
  )
}
