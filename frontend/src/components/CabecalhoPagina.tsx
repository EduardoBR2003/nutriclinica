import type { ReactNode } from 'react'

interface CabecalhoPaginaProps {
  titulo: string
  subtitulo?: string
  acao?: ReactNode
}

export function CabecalhoPagina({ titulo, subtitulo, acao }: CabecalhoPaginaProps) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-[18px]">
      <div className="min-w-0">
        <h1 className="text-6xl font-bold tracking-tighter">{titulo}</h1>
        {subtitulo ? (
          <p className="text-ink-muted mt-1.5 max-w-[70ch] text-lg leading-normal">{subtitulo}</p>
        ) : null}
      </div>
      {acao}
    </div>
  )
}
