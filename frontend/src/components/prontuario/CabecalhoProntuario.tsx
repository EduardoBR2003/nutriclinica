import { useNavigate } from 'react-router'

import { Avatar } from '@/components/Avatar'
import { Botao } from '@/components/Botao'
import { Chip } from '@/components/Chip'
import { ChipStatus } from '@/components/ChipStatus'
import { formatarData, formatarDataHora } from '@/lib/formato'
import type { AtendimentoCompleto } from '@/types/dominio'

interface CabecalhoProntuarioProps {
  atendimento: AtendimentoCompleto
  submetendo: boolean
  aoSubmeter: () => void
}

const TOTAL_SECOES = 11

export function CabecalhoProntuario({
  atendimento,
  submetendo,
  aoSubmeter,
}: CabecalhoProntuarioProps) {
  const navigate = useNavigate()
  const feitas = atendimento.secoesPreenchidas.length
  const percentual = Math.round((feitas / TOTAL_SECOES) * 100)

  return (
    <div className="bg-surface border-line-card shadow-card flex flex-wrap items-center justify-between gap-4 rounded-4xl border px-5 py-4">
      <div className="flex min-w-0 items-center gap-3">
        <Avatar nome={atendimento.paciente.nome} tamanho="lg" />
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2.5">
            <span className="text-4xl font-bold tracking-tight">{atendimento.paciente.nome}</span>
            <ChipStatus status={atendimento.status} />
            {!atendimento.editavel ? <Chip tom="info">somente leitura</Chip> : null}
          </div>
          <p className="text-ink-soft mt-1 text-base">
            Nº {atendimento.numeroProntuario} · consulta {formatarData(atendimento.dataConsulta)} ·
            estagiário {atendimento.estagiario.nome} · supervisor {atendimento.supervisor.nome}
          </p>
          <p className="text-ink-subtle mt-0.5 text-sm">
            {atendimento.submetidoEm
              ? `Submetido em ${formatarDataHora(atendimento.submetidoEm)}`
              : 'Ainda não submetido'}
            {atendimento.avaliadoEm
              ? ` · avaliado em ${formatarDataHora(atendimento.avaliadoEm)}`
              : ''}
          </p>
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-3.5">
        <div className="min-w-[160px]">
          <div className="mb-1.5 flex items-baseline justify-between gap-2.5">
            <span className="text-ink-muted text-sm">Seções preenchidas</span>
            <span className="text-brand-strong text-sm font-semibold">
              {feitas}/{TOTAL_SECOES}
            </span>
          </div>
          <div className="bg-line-subtle h-[5px] overflow-hidden rounded-full">
            <div
              className={feitas === TOTAL_SECOES ? 'bg-brand h-full' : 'bg-cta h-full'}
              style={{ width: `${percentual}%` }}
            />
          </div>
        </div>

        <Botao variante="contorno" onClick={() => navigate(-1)}>
          Voltar
        </Botao>

        {atendimento.editavel ? (
          <Botao variante="cta" onClick={aoSubmeter} disabled={submetendo}>
            {submetendo
              ? 'Enviando…'
              : atendimento.status === 'DEVOLVIDO_PARA_CORRECAO'
                ? 'Reenviar após correção'
                : 'Submeter para revisão'}
          </Botao>
        ) : null}
      </div>
    </div>
  )
}
