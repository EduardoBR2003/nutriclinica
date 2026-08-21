import type { ReactNode } from 'react'

import { cn } from '@/lib/utils'

export interface ColunaTabela {
  label: string
  alinhamento?: 'left' | 'right'
  /** Coluna de borda: ganha o respiro maior de 20px. */
  borda?: boolean
}

/** Rolagem horizontal própria — a tabela nunca empurra o body do design. */
export function Tabela({
  colunas,
  larguraMinima = 880,
  children,
}: {
  colunas: readonly ColunaTabela[]
  larguraMinima?: number
  children: ReactNode
}) {
  return (
    <div className="overflow-x-auto">
      <table
        className="w-full border-collapse text-lg"
        style={{ minWidth: `${larguraMinima}px` }}
      >
        <thead>
          <tr>
            {colunas.map((coluna, indice) => (
              <th
                key={coluna.label || `col-${indice}`}
                className={cn(
                  'bg-table-head border-line-subtle text-ink-muted border-b py-2.5 text-xs font-semibold tracking-wider uppercase',
                  coluna.alinhamento === 'right' ? 'text-right' : 'text-left',
                  coluna.borda ? 'px-5' : 'px-3',
                )}
              >
                {coluna.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>{children}</tbody>
      </table>
    </div>
  )
}

/** Linha com zebra e hover sálvia, como no design. */
export function LinhaTabela({ indice, children }: { indice: number; children: ReactNode }) {
  return (
    <tr className={cn('hover:bg-brand-row', indice % 2 ? 'bg-row-zebra' : 'bg-surface')}>
      {children}
    </tr>
  )
}

export function Celula({
  children,
  borda = false,
  alinhamento = 'left',
  className,
}: {
  children: ReactNode
  borda?: boolean
  alinhamento?: 'left' | 'right'
  className?: string
}) {
  return (
    <td
      className={cn(
        'border-line-faint border-b py-3',
        borda ? 'px-5' : 'px-3',
        alinhamento === 'right' ? 'text-right' : 'text-left',
        className,
      )}
    >
      {children}
    </td>
  )
}

export function EstadoVazio({ titulo, texto }: { titulo: string; texto?: string }) {
  return (
    <div className="px-5 py-9 text-center">
      <p className="text-2xl font-semibold">{titulo}</p>
      {texto ? <p className="text-ink-muted mt-1.5 text-md">{texto}</p> : null}
    </div>
  )
}
