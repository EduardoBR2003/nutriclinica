import type { ReactNode } from 'react'

import { Chip } from '@/components/Chip'
import { Painel } from '@/components/Painel'

interface CartaoSecaoProps {
  titulo: string
  descricao: string
  preenchida: boolean
  children: ReactNode
  rodape?: ReactNode
}

export function CartaoSecao({
  titulo,
  descricao,
  preenchida,
  children,
  rodape,
}: CartaoSecaoProps) {
  return (
    <Painel className="p-5">
      <div className="mb-4 flex flex-wrap items-start justify-between gap-3.5">
        <div className="min-w-0">
          <h2 className="text-3xl font-bold tracking-snug">{titulo}</h2>
          <p className="text-ink-muted mt-1 max-w-[62ch] text-base leading-normal">{descricao}</p>
        </div>
        {/* `secoesPreenchidas` vem do servidor — a UI não reinfere a regra. */}
        <Chip tom={preenchida ? 'sucesso' : 'alerta'}>
          {preenchida ? 'preenchida' : 'pendente'}
        </Chip>
      </div>
      {children}
      {rodape}
    </Painel>
  )
}
