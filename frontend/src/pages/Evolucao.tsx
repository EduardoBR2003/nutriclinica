import { useNavigate, useParams } from 'react-router'
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'

import { BannerErro } from '@/components/BannerErro'
import { Botao } from '@/components/Botao'
import { CabecalhoPagina } from '@/components/CabecalhoPagina'
import { Painel } from '@/components/Painel'
import { Celula, EstadoVazio, LinhaTabela, Tabela, type ColunaTabela } from '@/components/Tabela'
import { usePaciente, useEvolucao } from '@/hooks/usePacientes'
import { formatarData, formatarNumero } from '@/lib/formato'
import { cn } from '@/lib/utils'
import type { PontoEvolucao } from '@/types/dominio'

const COLUNAS: readonly ColunaTabela[] = [
  { label: 'Consulta', borda: true },
  { label: 'Peso' },
  { label: 'IMC' },
  { label: 'Cintura' },
  { label: 'Quadril', borda: true },
]

interface SerieDef {
  chave: keyof Omit<PontoEvolucao, 'dataConsulta'>
  label: string
  unidade: string
  casas: number
  cor: string
}

const SERIES: readonly SerieDef[] = [
  { chave: 'pesoKg', label: 'Peso', unidade: 'kg', casas: 1, cor: 'var(--color-chart-1)' },
  { chave: 'imc', label: 'IMC', unidade: 'kg/m²', casas: 1, cor: 'var(--color-chart-2)' },
  { chave: 'circCinturaCm', label: 'Cintura', unidade: 'cm', casas: 0, cor: 'var(--color-chart-3)' },
  { chave: 'circQuadrilCm', label: 'Quadril', unidade: 'cm', casas: 0, cor: 'var(--color-chart-4)' },
]

export default function Evolucao() {
  const { id } = useParams()
  const pacienteId = Number(id)
  const navigate = useNavigate()

  const paciente = usePaciente(pacienteId)
  const consulta = useEvolucao(pacienteId)
  const pontos = consulta.data ?? []

  const dadosGrafico = pontos.map((ponto) => ({
    ...ponto,
    rotulo: formatarData(ponto.dataConsulta),
  }))

  return (
    <>
      <CabecalhoPagina
        titulo="Evolução do paciente"
        subtitulo={
          paciente.data
            ? `${paciente.data.nome} · série de peso, IMC e circunferências por data de consulta.`
            : 'Série de peso, IMC e circunferências por data de consulta.'
        }
        acao={
          <Botao variante="contorno" tamanho="lg" onClick={() => navigate('/pacientes')}>
            Voltar
          </Botao>
        }
      />

      {consulta.isError ? <BannerErro erro={consulta.error} /> : null}

      {consulta.isPending ? (
        <Painel>
          <p className="text-ink-muted text-lg">Carregando evolução…</p>
        </Painel>
      ) : pontos.length === 0 ? (
        <Painel>
          <EstadoVazio
            titulo="Sem medidas registradas"
            // Só atendimento APROVADO entra na curva: rascunho e prontuário sob
            // revisão não devem ser lidos como histórico consolidado.
            texto="A série considera apenas atendimentos aprovados com antropometria aferida."
          />
        </Painel>
      ) : (
        <>
          <div className="grid grid-cols-[repeat(auto-fit,minmax(min(100%,220px),1fr))] gap-3.5">
            {SERIES.map((serie) => {
              const valores = pontos
                .map((ponto) => ponto[serie.chave])
                .filter((valor): valor is number => typeof valor === 'number')
              const atual = valores.at(-1)
              const primeiro = valores[0]
              // Com um único ponto não há evolução a mostrar — um "0,0" ali
              // sugeriria estabilidade que ninguém mediu.
              const delta =
                valores.length > 1 && atual !== undefined && primeiro !== undefined
                  ? atual - primeiro
                  : undefined

              return (
                <div
                  key={serie.chave}
                  className="bg-surface border-line-card shadow-card rounded-3xl border p-[18px]"
                >
                  <div className="flex items-baseline justify-between gap-2.5">
                    <span className="text-ink-soft text-base font-semibold">{serie.label}</span>
                    {delta !== undefined ? (
                      <span
                        className={cn(
                          'text-base font-semibold',
                          delta <= 0 ? 'text-success-text' : 'text-cta-text',
                        )}
                      >
                        {delta > 0 ? '+' : ''}
                        {formatarNumero(delta, serie.casas)}
                      </span>
                    ) : null}
                  </div>
                  <div className="mt-2.5 flex items-baseline gap-1.5">
                    <span className="text-7xl leading-none font-bold tracking-tightest">
                      {formatarNumero(atual, serie.casas)}
                    </span>
                    <span className="text-ink-subtle text-base">{serie.unidade}</span>
                  </div>
                </div>
              )
            })}
          </div>

          <Painel>
            <h2 className="mb-4 text-3xl font-bold tracking-snug">Série histórica</h2>
            <div className="h-[320px] w-full">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={dadosGrafico} margin={{ top: 8, right: 12, bottom: 4, left: -8 }}>
                  <CartesianGrid stroke="var(--color-line-subtle)" vertical={false} />
                  <XAxis
                    dataKey="rotulo"
                    tick={{ fontSize: 11, fill: 'var(--color-ink-muted)' }}
                    tickLine={false}
                    axisLine={{ stroke: 'var(--color-line-card)' }}
                  />
                  <YAxis
                    tick={{ fontSize: 11, fill: 'var(--color-ink-muted)' }}
                    tickLine={false}
                    axisLine={false}
                  />
                  <Tooltip
                    contentStyle={{
                      borderRadius: 12,
                      border: '1px solid var(--color-line-card)',
                      boxShadow: 'var(--shadow-raised)',
                      fontSize: 12.5,
                    }}
                  />
                  <Legend wrapperStyle={{ fontSize: 12.5 }} />
                  {SERIES.map((serie) => (
                    <Line
                      key={serie.chave}
                      type="monotone"
                      dataKey={serie.chave}
                      name={`${serie.label} (${serie.unidade})`}
                      stroke={serie.cor}
                      strokeWidth={2}
                      dot={{ r: 3 }}
                      connectNulls
                    />
                  ))}
                </LineChart>
              </ResponsiveContainer>
            </div>
          </Painel>

          <Painel semPadding>
            <Tabela colunas={COLUNAS} larguraMinima={620}>
              {[...pontos].reverse().map((ponto, indice) => (
                <LinhaTabela key={`${ponto.dataConsulta}-${indice}`} indice={indice}>
                  <Celula borda className="font-medium">
                    {formatarData(ponto.dataConsulta)}
                  </Celula>
                  <Celula className="text-ink-soft">{formatarNumero(ponto.pesoKg)} kg</Celula>
                  <Celula className="text-ink-soft">{formatarNumero(ponto.imc)}</Celula>
                  <Celula className="text-ink-soft">
                    {formatarNumero(ponto.circCinturaCm, 0)} cm
                  </Celula>
                  <Celula borda className="text-ink-soft">
                    {formatarNumero(ponto.circQuadrilCm, 0)} cm
                  </Celula>
                </LinhaTabela>
              ))}
            </Tabela>
          </Painel>
        </>
      )}
    </>
  )
}
