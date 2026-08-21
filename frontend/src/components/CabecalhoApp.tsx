import { useBuscaHeaderControle } from '@/hooks/useBusca'

/** Barra fixa do topo. A busca só aparece nas telas que a registram. */
export function CabecalhoApp() {
  const { termo, definirTermo, placeholder } = useBuscaHeaderControle()

  return (
    <header className="bg-canvas border-line-card sticky top-0 z-10 flex min-h-[68px] flex-wrap items-center gap-4 border-b px-6 py-3.5">
      {placeholder ? (
        <label className="bg-surface border-line-card shadow-hairline focus-within:border-brand-muted flex h-10 max-w-[420px] flex-[1_1_260px] items-center gap-2.5 rounded-xl border px-3.5">
          <svg
            width="17"
            height="17"
            viewBox="0 0 20 20"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.7"
            strokeLinecap="round"
            aria-hidden
            className="text-ink-muted shrink-0"
          >
            <circle cx="9" cy="9" r="6" />
            <path d="M13.5 13.5 17 17" />
          </svg>
          <input
            type="search"
            value={termo}
            onChange={(evento) => definirTermo(evento.target.value)}
            placeholder={placeholder}
            aria-label={placeholder}
            className="text-ink-body h-full min-w-0 flex-1 bg-transparent text-xl outline-none"
          />
        </label>
      ) : null}
    </header>
  )
}
