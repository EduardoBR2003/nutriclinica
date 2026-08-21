import { useNavigate } from 'react-router'

import { Botao } from '@/components/Botao'
import { ChipStatus } from '@/components/ChipStatus'
import { Paginacao } from '@/components/Paginacao'
import { Painel } from '@/components/Painel'
import { Celula, EstadoVazio, LinhaTabela, Tabela, type ColunaTabela } from '@/components/Tabela'
import { formatarData, formatarDataHora } from '@/lib/formato'
import type { Atendimento, Pagina } from '@/types/dominio'

const COLUNAS: readonly ColunaTabela[] = [
  { label: 'Prontuário', borda: true },
  { label: 'Paciente' },
  { label: 'Estagiário' },
  { label: 'Supervisor' },
  { label: 'Status' },
  { label: '', alinhamento: 'right', borda: true },
]

interface ListaAtendimentosProps {
  pagina: Pagina<Atendimento> | undefined
  carregando: boolean
  aoMudarPagina: (pagina: number) => void
  vazioTitulo: string
  vazioTexto: string
  /** Supervisor com a fila aberta troca o rótulo da ação para "Avaliar". */
  modoRevisao?: boolean
  cabecalho?: React.ReactNode
}

export function ListaAtendimentos({
  pagina,
  carregando,
  aoMudarPagina,
  vazioTitulo,
  vazioTexto,
  modoRevisao = false,
  cabecalho,
}: ListaAtendimentosProps) {
  const navigate = useNavigate()

  return (
    <Painel semPadding>
      {cabecalho}
      <Tabela colunas={COLUNAS}>
        {(pagina?.content ?? []).map((atendimento, indice) => {
          const revisar = modoRevisao && atendimento.status === 'EM_REVISAO'
          const destaque = revisar || atendimento.editavel
          return (
            <LinhaTabela key={atendimento.id} indice={indice}>
              <Celula borda>
                <p className="text-ink-label font-mono text-base font-semibold">
                  {atendimento.numeroProntuario}
                </p>
                <p className="text-ink-subtle mt-0.5 text-base">
                  Consulta {formatarData(atendimento.dataConsulta)}
                </p>
              </Celula>
              <Celula className="font-medium">{atendimento.paciente.nome}</Celula>
              <Celula className="text-ink-soft">{atendimento.estagiario.nome}</Celula>
              <Celula className="text-ink-soft">{atendimento.supervisor.nome}</Celula>
              <Celula>
                <ChipStatus status={atendimento.status} />
                <p className="text-ink-subtle mt-1 text-sm">
                  {atendimento.submetidoEm
                    ? `enviado ${formatarDataHora(atendimento.submetidoEm)}`
                    : 'não enviado'}
                  {atendimento.avaliadoEm
                    ? ` · avaliado ${formatarDataHora(atendimento.avaliadoEm)}`
                    : ''}
                </p>
              </Celula>
              <Celula borda alinhamento="right">
                <Botao
                  variante={destaque ? 'cta' : 'suave'}
                  tamanho="sm"
                  onClick={() => navigate(`/atendimentos/${atendimento.id}`)}
                >
                  {revisar ? 'Avaliar' : atendimento.editavel ? 'Continuar' : 'Ver prontuário'}
                </Botao>
              </Celula>
            </LinhaTabela>
          )
        })}
      </Tabela>

      {carregando ? (
        <EstadoVazio titulo="Carregando atendimentos…" />
      ) : pagina && pagina.content.length === 0 ? (
        <EstadoVazio titulo={vazioTitulo} texto={vazioTexto} />
      ) : null}

      {pagina ? (
        <Paginacao pagina={pagina} substantivo="atendimentos" aoMudar={aoMudarPagina} />
      ) : null}
    </Painel>
  )
}
