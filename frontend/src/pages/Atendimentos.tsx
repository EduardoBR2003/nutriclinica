import { useState } from 'react'
import { useNavigate } from 'react-router'

import { BannerErro } from '@/components/BannerErro'
import { Botao } from '@/components/Botao'
import { CabecalhoPagina } from '@/components/CabecalhoPagina'
import { FiltrosStatus, type FiltroStatus } from '@/components/FiltrosStatus'
import { ListaAtendimentos } from '@/components/ListaAtendimentos'
import { useAtendimentos } from '@/hooks/useAtendimentos'
import { useAuth } from '@/hooks/useAuth'
import { useBuscaHeader } from '@/hooks/useBusca'
import { usePacientes } from '@/hooks/usePacientes'

const OPCOES: readonly { valor: FiltroStatus; label: string }[] = [
  { valor: 'TODOS', label: 'Todos' },
  { valor: 'RASCUNHO', label: 'Rascunho' },
  { valor: 'EM_REVISAO', label: 'Em revisão' },
  { valor: 'DEVOLVIDO_PARA_CORRECAO', label: 'Devolvidos' },
  { valor: 'APROVADO', label: 'Aprovados' },
]

const TAMANHO_PAGINA = 10

export default function Atendimentos() {
  const { usuario } = useAuth()
  const navigate = useNavigate()
  const busca = useBuscaHeader('Buscar por paciente ou nº do prontuário…')
  const [status, setStatus] = useState<FiltroStatus>('TODOS')
  const [pacienteId, setPacienteId] = useState('')
  const [pagina, setPagina] = useState(0)

  const consulta = useAtendimentos({
    status: status === 'TODOS' ? undefined : status,
    pacienteId: pacienteId ? Number(pacienteId) : undefined,
    page: pagina,
    size: TAMANHO_PAGINA,
  })

  // O contrato não filtra a lista por texto; o filtro fica no cliente, sobre a
  // página corrente, e só sobre o que o servidor já autorizou o usuário a ver.
  const filtrada = consulta.data
    ? {
        ...consulta.data,
        content: consulta.data.content.filter((atendimento) => {
          const termo = busca.trim().toLowerCase()
          if (!termo) return true
          return (
            atendimento.numeroProntuario.toLowerCase().includes(termo) ||
            atendimento.paciente.nome.toLowerCase().includes(termo)
          )
        }),
      }
    : undefined

  const pacientes = usePacientes({ page: 0, size: 100 })

  return (
    <>
      <CabecalhoPagina
        titulo="Atendimentos"
        subtitulo="Seus prontuários por status. Rascunho e devolvido são editáveis."
        acao={
          usuario?.perfil === 'ESTAGIARIO' ? (
            <Botao variante="cta" tamanho="lg" onClick={() => navigate('/atendimentos/novo')}>
              + Novo atendimento
            </Botao>
          ) : undefined
        }
      />

      {consulta.isError ? <BannerErro erro={consulta.error} /> : null}

      <ListaAtendimentos
        pagina={filtrada}
        carregando={consulta.isPending}
        aoMudarPagina={setPagina}
        vazioTitulo="Nenhum atendimento"
        vazioTexto="Abra um atendimento para um paciente com termo registrado."
        cabecalho={
          <div className="border-line-subtle flex flex-wrap items-center justify-between gap-3.5 border-b px-5 pt-4 pb-3.5">
            <FiltrosStatus
              opcoes={OPCOES}
              atual={status}
              aoTrocar={(valor) => {
                setStatus(valor)
                setPagina(0)
              }}
            />
            <label className="flex items-center gap-2">
              <span className="text-ink-muted text-base whitespace-nowrap">Paciente</span>
              <select
                value={pacienteId}
                onChange={(evento) => {
                  setPacienteId(evento.target.value)
                  setPagina(0)
                }}
                className="text-ink-label bg-surface border-line-card h-[34px] cursor-pointer appearance-none rounded-lg border pr-8 pl-3 text-md font-medium outline-none"
              >
                <option value="">Todos</option>
                {(pacientes.data?.content ?? []).map((paciente) => (
                  <option key={paciente.id} value={paciente.id}>
                    {paciente.nome}
                  </option>
                ))}
              </select>
            </label>
          </div>
        }
      />
    </>
  )
}
