import { useState } from 'react'
import { Link, useNavigate } from 'react-router'

import { Avatar } from '@/components/Avatar'
import { BannerErro } from '@/components/BannerErro'
import { Botao, botaoVariantes } from '@/components/Botao'
import { CabecalhoPagina } from '@/components/CabecalhoPagina'
import { Chip } from '@/components/Chip'
import { Paginacao } from '@/components/Paginacao'
import { Painel } from '@/components/Painel'
import { Celula, EstadoVazio, LinhaTabela, Tabela, type ColunaTabela } from '@/components/Tabela'
import { useBuscaHeader } from '@/hooks/useBusca'
import { useUsuarios } from '@/hooks/useUsuarios'
import { PERFIL } from '@/lib/rotulos'
import { cn } from '@/lib/utils'
import type { Perfil } from '@/types/dominio'

const COLUNAS: readonly ColunaTabela[] = [
  { label: 'Usuário', borda: true },
  { label: 'Perfil' },
  { label: 'Situação' },
  { label: '', alinhamento: 'right', borda: true },
]

const FILTROS: readonly { valor: Perfil | 'TODOS'; label: string }[] = [
  { valor: 'TODOS', label: 'Todos' },
  { valor: 'ESTAGIARIO', label: 'Estagiários' },
  { valor: 'SUPERVISOR', label: 'Supervisores' },
  { valor: 'ADMIN', label: 'Administradores' },
]

const TAMANHO_PAGINA = 10

export default function Usuarios() {
  const navigate = useNavigate()
  const busca = useBuscaHeader('Buscar por nome ou e-mail…')
  const [perfil, setPerfil] = useState<Perfil | 'TODOS'>('TODOS')
  const [pagina, setPagina] = useState(0)

  const consulta = useUsuarios({
    perfil: perfil === 'TODOS' ? undefined : perfil,
    page: pagina,
    size: TAMANHO_PAGINA,
  })

  // O contrato não tem busca textual em /api/usuarios; filtra-se a página atual.
  const termo = busca.trim().toLowerCase()
  const visiveis = (consulta.data?.content ?? []).filter(
    (usuario) =>
      !termo ||
      usuario.nome.toLowerCase().includes(termo) ||
      usuario.email.toLowerCase().includes(termo),
  )

  return (
    <>
      <CabecalhoPagina
        titulo="Usuários"
        subtitulo="Professores, alunos e administradores da plataforma."
        acao={
          <Botao variante="cta" tamanho="lg" onClick={() => navigate('/admin/usuarios/novo')}>
            + Novo usuário
          </Botao>
        }
      />

      {consulta.isError ? <BannerErro erro={consulta.error} /> : null}

      <Painel semPadding>
        <div className="border-line-subtle flex flex-wrap gap-2 border-b px-5 pt-4 pb-3.5">
          {FILTROS.map((filtro) => {
            const ativo = perfil === filtro.valor
            return (
              <button
                key={filtro.valor}
                type="button"
                onClick={() => {
                  setPerfil(filtro.valor)
                  setPagina(0)
                }}
                aria-pressed={ativo}
                className={cn(
                  'inline-flex h-[34px] items-center rounded-lg border px-3.5 text-md font-semibold',
                  ativo
                    ? 'border-brand-border bg-brand-tint text-brand-text'
                    : 'border-line-card bg-surface text-ink-soft',
                )}
              >
                {filtro.label}
              </button>
            )
          })}
        </div>

        <Tabela colunas={COLUNAS} larguraMinima={760}>
          {visiveis.map((usuario, indice) => (
            <LinhaTabela key={usuario.id} indice={indice}>
              <Celula borda>
                <div className="flex items-center gap-2.5">
                  <Avatar nome={usuario.nome} />
                  <div className="min-w-0">
                    <p className="font-semibold">{usuario.nome}</p>
                    <p className="text-ink-subtle text-base">{usuario.email}</p>
                  </div>
                </div>
              </Celula>
              <Celula>
                <Chip
                  tom={
                    usuario.perfil === 'ADMIN'
                      ? 'info'
                      : usuario.perfil === 'SUPERVISOR'
                        ? 'brand'
                        : 'neutro'
                  }
                >
                  {PERFIL[usuario.perfil]}
                </Chip>
              </Celula>
              <Celula>
                <Chip tom={usuario.ativo ? 'sucesso' : 'neutro'}>
                  {usuario.ativo ? 'Ativo' : 'Inativo'}
                </Chip>
              </Celula>
              <Celula borda alinhamento="right">
                <Link
                  to={`/admin/usuarios/${usuario.id}`}
                  className={botaoVariantes({ variante: 'suave', tamanho: 'sm' })}
                >
                  Editar
                </Link>
              </Celula>
            </LinhaTabela>
          ))}
        </Tabela>

        {consulta.isPending ? (
          <EstadoVazio titulo="Carregando usuários…" />
        ) : consulta.isError ? (
          <EstadoVazio
            titulo="Lista indisponível"
            texto="Não foi possível carregar os usuários. Veja o detalhe do erro acima."
          />
        ) : visiveis.length === 0 ? (
          <EstadoVazio
            titulo="Nenhum usuário encontrado"
            texto="Ajuste a busca ou o filtro de perfil."
          />
        ) : null}

        {consulta.data ? (
          <Paginacao pagina={consulta.data} substantivo="usuários" aoMudar={setPagina} />
        ) : null}
      </Painel>
    </>
  )
}
