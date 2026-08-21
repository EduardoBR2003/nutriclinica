import { useState } from 'react'

import { BannerErro } from '@/components/BannerErro'
import { CabecalhoPagina } from '@/components/CabecalhoPagina'
import { FiltrosStatus, type FiltroStatus } from '@/components/FiltrosStatus'
import { ListaAtendimentos } from '@/components/ListaAtendimentos'
import { useAtendimentos } from '@/hooks/useAtendimentos'
import { useRevisoesPendentes } from '@/hooks/useRevisoes'

const OPCOES: readonly { valor: FiltroStatus; label: string }[] = [
  { valor: 'EM_REVISAO', label: 'Em revisão' },
  { valor: 'APROVADO', label: 'Aprovados' },
  { valor: 'DEVOLVIDO_PARA_CORRECAO', label: 'Devolvidos' },
  { valor: 'TODOS', label: 'Todos' },
]

const TAMANHO_PAGINA = 10

export default function Revisoes() {
  const [status, setStatus] = useState<FiltroStatus>('EM_REVISAO')
  const [pagina, setPagina] = useState(0)

  // "Em revisão" tem endpoint próprio: é a fila do supervisor *designado*,
  // ordenada do mais antigo — orientar o estagiário não basta para revisar.
  const naFila = status === 'EM_REVISAO'
  const pendentes = useRevisoesPendentes(pagina, TAMANHO_PAGINA)
  const outros = useAtendimentos({
    status: status === 'TODOS' ? undefined : status,
    page: pagina,
    size: TAMANHO_PAGINA,
  })

  const consulta = naFila ? pendentes : outros

  return (
    <>
      <CabecalhoPagina
        titulo="Fila de revisão"
        subtitulo="Prontuários enviados, do mais antigo para o mais recente."
      />

      {consulta.isError ? <BannerErro erro={consulta.error} /> : null}

      <ListaAtendimentos
        pagina={consulta.data}
        carregando={consulta.isPending}
        aoMudarPagina={setPagina}
        modoRevisao
        vazioTitulo="Fila vazia"
        vazioTexto="Nenhum prontuário aguardando seu parecer neste filtro."
        cabecalho={
          <div className="border-line-subtle border-b px-5 pt-4 pb-3.5">
            <FiltrosStatus
              opcoes={OPCOES}
              atual={status}
              aoTrocar={(valor) => {
                setStatus(valor)
                setPagina(0)
              }}
            />
          </div>
        }
      />
    </>
  )
}
