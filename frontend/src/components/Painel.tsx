import type { ReactNode } from 'react'

import { cn } from '@/lib/utils'

/** Cartão branco de conteúdo — a superfície base de quase toda tela do design. */
export function Painel({
  children,
  className,
  semPadding = false,
}: {
  children: ReactNode
  className?: string
  semPadding?: boolean
}) {
  return (
    <section
      className={cn(
        'bg-surface border-line-card shadow-panel rounded-4xl border',
        semPadding ? 'overflow-hidden' : 'p-[22px]',
        className,
      )}
    >
      {children}
    </section>
  )
}
