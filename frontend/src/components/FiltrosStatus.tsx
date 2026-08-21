import { cn } from '@/lib/utils'
import type { StatusAtendimento } from '@/types/dominio'

export type FiltroStatus = StatusAtendimento | 'TODOS'

interface FiltrosStatusProps {
  opcoes: readonly { valor: FiltroStatus; label: string }[]
  atual: FiltroStatus
  aoTrocar: (valor: FiltroStatus) => void
}

export function FiltrosStatus({ opcoes, atual, aoTrocar }: FiltrosStatusProps) {
  return (
    <div className="flex flex-wrap gap-2">
      {opcoes.map((opcao) => {
        const ativo = atual === opcao.valor
        return (
          <button
            key={opcao.valor}
            type="button"
            onClick={() => aoTrocar(opcao.valor)}
            aria-pressed={ativo}
            className={cn(
              'inline-flex h-[34px] items-center gap-2 rounded-lg border px-3.5 text-md font-semibold whitespace-nowrap',
              ativo
                ? 'border-brand-border bg-brand-tint text-brand-text'
                : 'border-line-card bg-surface text-ink-soft',
            )}
          >
            {opcao.label}
          </button>
        )
      })}
    </div>
  )
}
