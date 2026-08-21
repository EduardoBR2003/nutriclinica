import { cn } from '@/lib/utils'

interface PaginacaoProps {
  /** Vem direto do schema `Pagina` do contrato. */
  pagina: { number: number; totalPages: number; totalElements: number }
  substantivo: string
  aoMudar: (pagina: number) => void
}

const BOTAO_BASE = 'h-8 rounded-md border px-3.5 text-base font-semibold'

export function Paginacao({ pagina, substantivo, aoMudar }: PaginacaoProps) {
  const totalPaginas = Math.max(1, pagina.totalPages)
  const atual = Math.min(pagina.number, totalPaginas - 1)
  const temAnterior = atual > 0
  const temProxima = atual < totalPaginas - 1

  const estilo = (ativo: boolean) =>
    cn(
      BOTAO_BASE,
      ativo
        ? 'text-ink-soft bg-surface border-line-card cursor-pointer'
        : 'text-ink-muted bg-[oklch(0.972_0.004_262)] border-line-subtle cursor-not-allowed',
    )

  return (
    <div className="border-line-faint flex flex-wrap items-center justify-between gap-3.5 border-t px-5 py-3">
      <span className="text-ink-muted text-base">
        Página {atual + 1} de {totalPaginas} · {pagina.totalElements} {substantivo}
      </span>
      <div className="flex gap-2">
        <button
          type="button"
          disabled={!temAnterior}
          onClick={() => aoMudar(atual - 1)}
          className={estilo(temAnterior)}
        >
          Anterior
        </button>
        <button
          type="button"
          disabled={!temProxima}
          onClick={() => aoMudar(atual + 1)}
          className={estilo(temProxima)}
        >
          Próxima
        </button>
      </div>
    </div>
  )
}
