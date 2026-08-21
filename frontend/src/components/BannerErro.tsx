import { extrairErro } from '@/lib/erros'

interface BannerErroProps {
  erro: unknown
  aoFechar?: () => void
}

/**
 * Banner de erro no formato do schema `Erro`: código estável, status HTTP,
 * mensagem e um chip por item de `campos` — que é como o 409
 * `SECOES_INCOMPLETAS` diz exatamente o que falta para submeter.
 */
export function BannerErro({ erro, aoFechar }: BannerErroProps) {
  if (!erro) return null
  const { codigo, mensagem, status, campos } = extrairErro(erro)

  return (
    <div
      role="alert"
      className="bg-danger-tint border-danger-border flex gap-3 rounded-3xl border p-4"
    >
      <span className="bg-cta mt-px inline-flex size-[22px] shrink-0 items-center justify-center rounded-full text-md font-bold text-white">
        !
      </span>
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          {codigo ? (
            <span className="text-cta-text-deep font-mono text-xs font-bold tracking-wide">
              {codigo}
            </span>
          ) : null}
          {status ? <span className="text-cta-text text-xs">HTTP {status}</span> : null}
        </div>
        <p className="text-danger-text mt-1.5 text-lg leading-relaxed font-medium">{mensagem}</p>
        {campos.length > 0 ? (
          <ul className="mt-2.5 flex flex-wrap gap-1.5">
            {campos.map((campo) => (
              <li
                key={`${campo.campo}-${campo.mensagem}`}
                title={campo.mensagem}
                className="border-cta-border text-cta-text inline-flex h-[22px] items-center rounded-sm border bg-white px-2 font-mono text-xs font-semibold"
              >
                {campo.campo}
              </li>
            ))}
          </ul>
        ) : null}
      </div>
      {aoFechar ? (
        <button
          type="button"
          onClick={aoFechar}
          aria-label="Fechar aviso"
          className="border-cta-border text-cta-text inline-flex size-7 shrink-0 items-center justify-center rounded-[9px] border bg-white text-md"
        >
          ×
        </button>
      ) : null}
    </div>
  )
}
