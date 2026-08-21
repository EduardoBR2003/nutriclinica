import { Chip, type TomChip } from '@/components/Chip'
import { formatarNumero } from '@/lib/formato'
import { CLASSIFICACAO_IMC, RISCO_CARDIOVASCULAR } from '@/lib/rotulos'
import type { Antropometria } from '@/types/dominio'

/**
 * IMC, classificação OMS, relação cintura/quadril e risco cardiovascular são
 * calculados no backend. Aqui só se exibe o que o `PATCH` devolveu — recalcular
 * no cliente duplicaria regra clínica e abriria espaço para divergência.
 */
export function PainelCalculados({ dados }: { dados: Antropometria | undefined }) {
  const imc = dados?.imc
  const rcq = dados?.relacaoCinturaQuadril
  const risco = dados?.riscoCardiovascular
  const classificacao = dados?.classificacaoImc
    ? (CLASSIFICACAO_IMC[dados.classificacaoImc] ?? dados.classificacaoImc)
    : undefined

  const cartoes: {
    label: string
    valor: string
    unidade?: string
    nota: string
    tom: TomChip
    grande: boolean
  }[] = [
    {
      label: 'IMC',
      valor: formatarNumero(imc),
      unidade: 'kg/m²',
      nota: classificacao ?? 'aguardando medidas',
      tom: imc === undefined ? 'neutro' : imc < 25 ? 'sucesso' : 'alerta',
      grande: true,
    },
    {
      label: 'Classificação IMC',
      valor: classificacao ?? '—',
      nota: imc === undefined ? 'aguardando medidas' : 'OMS adultos',
      tom: 'neutro',
      grande: false,
    },
    {
      label: 'Cintura/quadril',
      valor: formatarNumero(rcq, 2),
      nota: rcq === undefined ? '—' : dados?.rcqElevada ? 'RCQ elevada' : 'Adequada',
      tom: rcq === undefined ? 'neutro' : dados?.rcqElevada ? 'alerta' : 'sucesso',
      grande: true,
    },
    {
      label: 'Risco cardiovascular',
      valor: risco ? RISCO_CARDIOVASCULAR[risco] : '—',
      nota:
        dados?.circCinturaCm === undefined || dados.circCinturaCm === null
          ? '—'
          : `${formatarNumero(dados.circCinturaCm, 0)} cm de cintura`,
      tom: !risco ? 'neutro' : risco === 'BAIXO' ? 'sucesso' : 'alerta',
      grande: false,
    },
  ]

  return (
    <div className="border-line-subtle mt-4 border-t pt-4">
      <p className="text-ink-muted mb-3 text-base font-bold tracking-wide uppercase">
        Calculado pelo servidor
      </p>
      <div className="grid grid-cols-[repeat(auto-fit,minmax(min(100%,175px),1fr))] gap-3">
        {cartoes.map((cartao) => (
          <div
            key={cartao.label}
            className="bg-brand-surface border-brand-border rounded-2xl border px-4 py-3.5"
          >
            <p className="text-brand-strong text-xs leading-tight font-semibold tracking-wide uppercase">
              {cartao.label}
            </p>
            <div className="mt-2 flex items-baseline gap-1.5">
              <span
                className={
                  cartao.grande
                    ? 'text-ink text-7xl leading-none font-bold tracking-tightest'
                    : 'text-ink text-2xl leading-tight font-bold tracking-snug'
                }
              >
                {cartao.valor}
              </span>
              {cartao.unidade ? (
                <span className="text-brand-strong text-sm">{cartao.unidade}</span>
              ) : null}
            </div>
            <div className="mt-2">
              <Chip tom={cartao.tom} className="bg-surface">
                {cartao.nota}
              </Chip>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
