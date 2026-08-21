import type { ReactNode } from 'react'

import { cn } from '@/lib/utils'

export type TomChip = 'neutro' | 'brand' | 'sucesso' | 'alerta' | 'info'

const TONS: Record<TomChip, string> = {
  neutro: 'text-ink-soft bg-disabled border-line-card',
  brand: 'text-brand-text bg-brand-tint border-brand-border',
  sucesso: 'text-success-text bg-success-tint border-success-border',
  alerta: 'text-cta-text bg-cta-tint border-cta-border',
  info: 'text-info-text bg-info-tint border-info-border',
}

interface ChipProps {
  tom?: TomChip
  children: ReactNode
  className?: string
}

/** Etiqueta arredondada do design — termo, perfil, situação, contagem. */
export function Chip({ tom = 'neutro', children, className }: ChipProps) {
  return (
    <span
      className={cn(
        'inline-flex h-6 items-center gap-1.5 rounded-full border px-2.5 text-sm font-semibold whitespace-nowrap',
        TONS[tom],
        className,
      )}
    >
      {children}
    </span>
  )
}
