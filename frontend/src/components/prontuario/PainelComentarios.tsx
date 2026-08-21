import { useState } from 'react'

import { Avatar } from '@/components/Avatar'
import { BannerErro } from '@/components/BannerErro'
import { Botao } from '@/components/Botao'
import { Chip } from '@/components/Chip'
import { useAdicionarComentario, useComentarios } from '@/hooks/useRevisoes'
import { useToast } from '@/hooks/useToast'
import { formatarDataHora } from '@/lib/formato'
import { cn } from '@/lib/utils'
import type { SecaoProntuario } from '@/types/dominio'

interface PainelComentariosProps {
  atendimentoId: number
  secao: SecaoProntuario
  tituloSecao: string
  /** Só o supervisor designado escreve; o estagiário dono apenas lê. */
  podeComentar: boolean
}

export function PainelComentarios({
  atendimentoId,
  secao,
  tituloSecao,
  podeComentar,
}: PainelComentariosProps) {
  const [texto, setTexto] = useState('')
  const { avisar } = useToast()
  const consulta = useComentarios(atendimentoId)
  const adicionar = useAdicionarComentario(atendimentoId)

  const daSecao = (consulta.data ?? []).filter((comentario) => comentario.secao === secao)
  const abertos = daSecao.filter((comentario) => !comentario.resolvido).length

  async function enviar() {
    const conteudo = texto.trim()
    if (!conteudo) return
    await adicionar.mutateAsync({ secao, texto: conteudo })
    setTexto('')
    avisar('Comentário adicionado', `O estagiário vê a pendência em ${tituloSecao}.`)
  }

  return (
    <section className="bg-surface border-line-card shadow-panel overflow-hidden rounded-4xl border">
      <div className="flex flex-wrap items-center justify-between gap-3 px-[18px] pt-4 pb-3">
        <div>
          <h2 className="text-2xl font-bold tracking-snug">Comentários do supervisor</h2>
          <p className="text-ink-muted mt-0.5 text-base">Seção: {tituloSecao}</p>
        </div>
        <Chip tom={abertos ? 'alerta' : 'neutro'}>
          {abertos ? `${abertos} em aberto` : 'sem pendências'}
        </Chip>
      </div>

      <div className="flex flex-col gap-2.5 px-[18px] pb-4">
        {consulta.isError ? <BannerErro erro={consulta.error} /> : null}

        {daSecao.map((comentario) => (
          <article
            key={comentario.id}
            className={cn(
              'rounded-2xl border px-4 py-3.5',
              comentario.resolvido
                ? 'bg-surface border-line-card'
                : 'bg-cta-tint border-cta-border',
            )}
          >
            <div className="flex flex-wrap items-center justify-between gap-2.5">
              <div className="flex min-w-0 items-center gap-2.5">
                <Avatar nome={comentario.autor?.nome} tamanho="sm" />
                <div className="min-w-0">
                  <p className="text-base leading-tight font-semibold">
                    {comentario.autor?.nome ?? '—'}
                  </p>
                  <p className="text-ink-subtle mt-px text-xs">
                    {formatarDataHora(comentario.criadoEm)}
                  </p>
                </div>
              </div>
              {/* O contrato tem o campo, mas não há endpoint para alternar. */}
              <Chip tom={comentario.resolvido ? 'sucesso' : 'alerta'}>
                {comentario.resolvido ? 'resolvido' : 'em aberto'}
              </Chip>
            </div>
            <p className="text-ink-body mt-2.5 text-md leading-relaxed">{comentario.texto}</p>
          </article>
        ))}

        {!consulta.isPending && daSecao.length === 0 ? (
          <p className="bg-surface-sunken border-line text-ink-muted rounded-2xl border border-dashed p-4 text-center text-base leading-normal">
            Nenhum comentário nesta seção.
          </p>
        ) : null}
      </div>

      {podeComentar ? (
        <div className="bg-surface-alt border-line-subtle border-t px-[18px] py-4">
          <label
            htmlFor="novo-comentario"
            className="text-ink-label mb-2 block text-base font-semibold"
          >
            Novo comentário nesta seção
          </label>
          <textarea
            id="novo-comentario"
            rows={3}
            value={texto}
            onChange={(evento) => setTexto(evento.target.value)}
            placeholder="Descreva objetivamente o que precisa ser corrigido."
            className="text-ink bg-surface border-line focus:border-brand-muted focus:ring-brand-tint-hover w-full resize-y rounded-xl border px-3 py-2.5 text-md leading-relaxed outline-none focus:ring-3"
          />
          {adicionar.isError ? (
            <div className="mt-2.5">
              <BannerErro erro={adicionar.error} />
            </div>
          ) : null}
          <div className="mt-2.5 flex justify-end">
            <Botao
              type="button"
              tamanho="sm"
              onClick={() => void enviar()}
              disabled={adicionar.isPending || !texto.trim()}
            >
              {adicionar.isPending ? 'Enviando…' : 'Adicionar comentário'}
            </Botao>
          </div>
        </div>
      ) : null}
    </section>
  )
}
