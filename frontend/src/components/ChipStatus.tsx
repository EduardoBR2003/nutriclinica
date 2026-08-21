import { Chip, type TomChip } from '@/components/Chip'
import { STATUS_ATENDIMENTO } from '@/lib/rotulos'
import { cn } from '@/lib/utils'
import type { StatusAtendimento } from '@/types/dominio'

const TOM: Record<StatusAtendimento, TomChip> = {
  RASCUNHO: 'neutro',
  EM_REVISAO: 'info',
  APROVADO: 'sucesso',
  DEVOLVIDO_PARA_CORRECAO: 'alerta',
}

const PONTO: Record<StatusAtendimento, string> = {
  RASCUNHO: 'bg-control-off',
  // Anel vazado: em revisão é o único estado "em andamento".
  EM_REVISAO: 'border-2 border-info bg-transparent',
  APROVADO: 'bg-success',
  DEVOLVIDO_PARA_CORRECAO: 'bg-cta',
}

export function ChipStatus({ status }: { status: StatusAtendimento }) {
  return (
    <Chip tom={TOM[status]}>
      <span className={cn('size-[7px] shrink-0 rounded-full', PONTO[status])} />
      {STATUS_ATENDIMENTO[status]}
    </Chip>
  )
}
