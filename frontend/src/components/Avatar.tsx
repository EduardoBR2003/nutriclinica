import { iniciais } from '@/lib/formato'
import { cn } from '@/lib/utils'

interface AvatarProps {
  nome: string | undefined | null
  tamanho?: 'sm' | 'md' | 'lg'
  className?: string
}

const TAMANHOS = {
  sm: 'size-7 rounded-[9px] text-xs',
  md: 'size-9 rounded-xl text-base',
  lg: 'size-[42px] rounded-2xl text-xl',
}

/** Quadrado com as iniciais — o design não usa foto em lugar nenhum. */
export function Avatar({ nome, tamanho = 'md', className }: AvatarProps) {
  return (
    <span
      aria-hidden
      className={cn(
        'bg-brand-tint text-brand-text inline-flex shrink-0 items-center justify-center font-bold',
        TAMANHOS[tamanho],
        className,
      )}
    >
      {iniciais(nome)}
    </span>
  )
}
