import { cva, type VariantProps } from 'class-variance-authority'
import type { ComponentProps } from 'react'

import { cn } from '@/lib/utils'

/**
 * Botões do design. Convivem com o `ui/button.tsx` do shadcn, que é bem mais
 * compacto (h-8) do que os controles desenhados aqui (h-40/42/46).
 */
const botaoVariantes = cva(
  'inline-flex items-center justify-center gap-2 font-semibold whitespace-nowrap transition-colors disabled:cursor-not-allowed disabled:opacity-60',
  {
    variants: {
      variante: {
        primario: 'bg-brand text-white hover:bg-brand-hover',
        cta: 'bg-cta text-white shadow-cta hover:bg-cta-hover',
        contorno: 'bg-surface text-ink-label border-line-card border hover:bg-[oklch(0.965_0.004_262)]',
        suave: 'bg-brand-tint text-brand-text border-brand-border border hover:bg-brand-tint-hover',
        fantasma: 'text-ink-soft hover:bg-surface-alt',
      },
      tamanho: {
        sm: 'h-8 rounded-md px-3 text-base',
        md: 'h-10 rounded-xl px-4 text-lg',
        lg: 'h-[42px] rounded-xl px-5 text-xl',
        bloco: 'h-[46px] w-full rounded-xl px-5 text-xl',
      },
    },
    defaultVariants: { variante: 'primario', tamanho: 'md' },
  },
)

export type BotaoProps = ComponentProps<'button'> & VariantProps<typeof botaoVariantes>

export function Botao({ className, variante, tamanho, ...props }: BotaoProps) {
  return <button className={cn(botaoVariantes({ variante, tamanho }), className)} {...props} />
}

export { botaoVariantes }
