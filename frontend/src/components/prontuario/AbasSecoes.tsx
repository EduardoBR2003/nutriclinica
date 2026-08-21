import { GRUPOS, META_SECAO } from '@/components/prontuario/secoes'
import { cn } from '@/lib/utils'
import { SECOES, type SecaoProntuario } from '@/types/dominio'

interface AbasSecoesProps {
  atual: SecaoProntuario
  preenchidas: readonly SecaoProntuario[]
  /** Quantos comentários em aberto por seção — acende o ponto laranja. */
  pendencias: Partial<Record<SecaoProntuario, number>>
  aoTrocar: (secao: SecaoProntuario) => void
}

export function AbasSecoes({ atual, preenchidas, pendencias, aoTrocar }: AbasSecoesProps) {
  return (
    <nav
      aria-label="Seções do prontuário"
      className="bg-surface border-line-card flex flex-wrap gap-5 rounded-3xl border px-[18px] py-3.5"
    >
      {GRUPOS.map((grupo) => (
        <div key={grupo} className="flex min-w-0 flex-col gap-2">
          <span className="text-ink-faint text-2xs font-semibold tracking-widest uppercase">
            {grupo}
          </span>
          <div className="flex flex-wrap gap-1.5">
            {SECOES.filter((secao) => META_SECAO[secao].grupo === grupo).map((secao) => {
              const ativo = secao === atual
              const pendente = (pendencias[secao] ?? 0) > 0
              const vazia = !preenchidas.includes(secao)
              return (
                <button
                  key={secao}
                  type="button"
                  onClick={() => aoTrocar(secao)}
                  aria-current={ativo ? 'true' : undefined}
                  className={cn(
                    'inline-flex h-8 items-center gap-2 rounded-md border px-3 text-base whitespace-nowrap',
                    ativo
                      ? 'border-brand-border bg-brand-tint text-brand-text font-semibold'
                      : 'border-line-card bg-surface text-ink-soft font-medium',
                  )}
                >
                  {META_SECAO[secao].titulo}
                  {pendente ? (
                    <span className="bg-cta size-1.5 shrink-0 rounded-full" title="Comentário em aberto" />
                  ) : vazia ? (
                    <span className="bg-control-off size-1.5 shrink-0 rounded-full" title="Seção pendente" />
                  ) : null}
                </button>
              )
            })}
          </div>
        </div>
      ))}
    </nav>
  )
}
