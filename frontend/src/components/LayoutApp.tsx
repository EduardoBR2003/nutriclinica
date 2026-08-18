import { NavLink, Outlet } from 'react-router'

import { Button } from '@/components/ui/button'
import { useAuth } from '@/hooks/useAuth'
import { cn } from '@/lib/utils'
import type { Perfil } from '@/types/dominio'

const LINKS: { para: string; rotulo: string; perfis: readonly Perfil[] }[] = [
  { para: '/pacientes', rotulo: 'Pacientes', perfis: ['ESTAGIARIO', 'SUPERVISOR', 'ADMIN'] },
  { para: '/atendimentos', rotulo: 'Atendimentos', perfis: ['ESTAGIARIO', 'SUPERVISOR'] },
  { para: '/revisoes', rotulo: 'Revisões', perfis: ['SUPERVISOR'] },
  { para: '/admin/usuarios', rotulo: 'Usuários', perfis: ['ADMIN'] },
]

export function LayoutApp() {
  const { usuario, logout } = useAuth()
  if (!usuario) return null

  return (
    <div className="min-h-svh">
      <header className="border-b">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center gap-4 px-6 py-3">
          <span className="font-heading font-semibold">NutriClinica</span>

          <nav className="flex gap-4 text-sm">
            {LINKS.filter((link) => link.perfis.includes(usuario.perfil)).map((link) => (
              <NavLink
                key={link.para}
                to={link.para}
                className={({ isActive }) =>
                  cn(
                    'hover:text-foreground transition-colors',
                    isActive ? 'text-foreground font-medium' : 'text-muted-foreground',
                  )
                }
              >
                {link.rotulo}
              </NavLink>
            ))}
          </nav>

          <div className="ml-auto flex items-center gap-3">
            <span className="text-muted-foreground text-sm">
              {usuario.nome} · {usuario.perfil}
            </span>
            <Button variant="outline" size="sm" onClick={logout}>
              Sair
            </Button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-5xl px-6 py-8">
        <Outlet />
      </main>
    </div>
  )
}
