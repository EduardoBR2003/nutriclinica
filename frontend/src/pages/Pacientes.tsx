import { useState } from 'react'
import { Link, useNavigate } from 'react-router'

import { BannerErro } from '@/components/BannerErro'
import { Avatar } from '@/components/Avatar'
import { Botao, botaoVariantes } from '@/components/Botao'
import { CabecalhoPagina } from '@/components/CabecalhoPagina'
import { Chip } from '@/components/Chip'
import { Paginacao } from '@/components/Paginacao'
import { Painel } from '@/components/Painel'
import { Celula, EstadoVazio, LinhaTabela, Tabela, type ColunaTabela } from '@/components/Tabela'
import { useAuth } from '@/hooks/useAuth'
import { useBuscaHeader } from '@/hooks/useBusca'
import { usePacientes } from '@/hooks/usePacientes'
import { formatarData } from '@/lib/formato'
import { RACA_COR, SEXO } from '@/lib/rotulos'

const COLUNAS: readonly ColunaTabela[] = [
  { label: 'Paciente', borda: true },
  { label: 'Idade' },
  { label: 'Sexo' },
  { label: 'Raça/cor' },
  { label: 'Contato' },
  { label: 'Termo' },
  { label: '', alinhamento: 'right', borda: true },
]

const TAMANHO_PAGINA = 10

export default function Pacientes() {
  const { usuario } = useAuth()
  const navigate = useNavigate()
  const busca = useBuscaHeader('Buscar por nome ou telefone…')
  const [pagina, setPagina] = useState(0)

  // Trocar o termo precisa voltar para a primeira página, senão a busca some.
  const [buscaAnterior, setBuscaAnterior] = useState(busca)
  if (busca !== buscaAnterior) {
    setBuscaAnterior(busca)
    setPagina(0)
  }

  const consulta = usePacientes({ busca, page: pagina, size: TAMANHO_PAGINA })
  const podeCadastrar = usuario?.perfil !== 'SUPERVISOR'

  return (
    <>
      <CabecalhoPagina
        titulo="Pacientes"
        subtitulo="Busca por nome ou telefone, com termo de consentimento e evolução por paciente."
        acao={
          podeCadastrar ? (
            <Botao variante="cta" tamanho="lg" onClick={() => navigate('/pacientes/novo')}>
              + Novo paciente
            </Botao>
          ) : undefined
        }
      />

      {consulta.isError ? <BannerErro erro={consulta.error} /> : null}

      <Painel semPadding>
        <Tabela colunas={COLUNAS} larguraMinima={900}>
          {(consulta.data?.content ?? []).map((paciente, indice) => (
            <LinhaTabela key={paciente.id} indice={indice}>
              <Celula borda>
                <div className="flex min-w-0 items-center gap-3">
                  <Avatar nome={paciente.nome} />
                  <div className="min-w-0">
                    <p className="font-semibold">{paciente.nome}</p>
                    <p className="text-ink-subtle mt-0.5 text-base">
                      {formatarData(paciente.dataNascimento)}
                    </p>
                  </div>
                </div>
              </Celula>
              <Celula className="text-ink-soft">{paciente.idade} anos</Celula>
              <Celula className="text-ink-soft">{SEXO[paciente.sexo]}</Celula>
              <Celula className="text-ink-soft">
                {paciente.racaCor ? RACA_COR[paciente.racaCor] : '—'}
              </Celula>
              <Celula className="text-ink-soft">
                <p>{paciente.telefone || '—'}</p>
                <p className="text-ink-subtle mt-0.5 text-base">{paciente.email || '—'}</p>
              </Celula>
              <Celula>
                <Chip tom={paciente.possuiTermo ? 'sucesso' : 'alerta'}>
                  {paciente.possuiTermo ? 'registrado' : 'sem termo'}
                </Chip>
              </Celula>
              <Celula borda alinhamento="right">
                <div className="flex flex-wrap justify-end gap-2">
                  <Botao
                    variante={paciente.possuiTermo ? 'suave' : 'cta'}
                    tamanho="sm"
                    onClick={() => navigate(`/pacientes/${paciente.id}/termo`)}
                  >
                    {paciente.possuiTermo ? 'Ver termo' : 'Registrar termo'}
                  </Botao>
                  <Botao
                    variante="contorno"
                    tamanho="sm"
                    onClick={() => navigate(`/pacientes/${paciente.id}/evolucao`)}
                  >
                    Evolução
                  </Botao>
                  {podeCadastrar ? (
                    <Link
                      to={`/pacientes/${paciente.id}/editar`}
                      className={botaoVariantes({ variante: 'fantasma', tamanho: 'sm' })}
                    >
                      Editar
                    </Link>
                  ) : null}
                </div>
              </Celula>
            </LinhaTabela>
          ))}
        </Tabela>

        {consulta.isPending ? (
          <EstadoVazio titulo="Carregando pacientes…" />
        ) : consulta.data && consulta.data.content.length === 0 ? (
          <EstadoVazio
            titulo="Nenhum paciente encontrado"
            texto="Ajuste a busca ou cadastre um novo paciente."
          />
        ) : null}

        {consulta.data ? (
          <Paginacao pagina={consulta.data} substantivo="pacientes" aoMudar={setPagina} />
        ) : null}
      </Painel>
    </>
  )
}
