import { useCallback, useMemo, useState } from 'react'
import { Outlet } from 'react-router'

import { CabecalhoApp } from '@/components/CabecalhoApp'
import { SidebarApp } from '@/components/SidebarApp'
import { useAuth } from '@/hooks/useAuth'
import { BuscaContext } from '@/lib/buscaContext'

/** Casca do app: sidebar fixa de 250px + cabeçalho grudento + conteúdo. */
export function LayoutApp() {
  const { usuario } = useAuth()
  const [termo, definirTermo] = useState('')
  const [placeholder, definirPlaceholder] = useState<string | null>(null)

  const registrar = useCallback((novo: string) => definirPlaceholder(novo), [])
  const desregistrar = useCallback(() => {
    definirPlaceholder(null)
    definirTermo('')
  }, [])

  const busca = useMemo(
    () => ({ termo, definirTermo, placeholder, registrar, desregistrar }),
    [termo, placeholder, registrar, desregistrar],
  )

  // RotaProtegida já barrou o anônimo; aqui é só defesa.
  if (!usuario) return null

  return (
    <BuscaContext value={busca}>
      <div className="grid min-h-svh grid-cols-[250px_minmax(0,1fr)] items-start">
        <SidebarApp />
        <div className="min-w-0">
          <CabecalhoApp />
          <main className="flex flex-col gap-5 px-6 pt-6 pb-15">
            <Outlet />
          </main>
        </div>
      </div>
    </BuscaContext>
  )
}
