import { useEffect, useState } from 'react'

import { BannerErro } from '@/components/BannerErro'
import { Botao } from '@/components/Botao'
import { CabecalhoPagina } from '@/components/CabecalhoPagina'
import { Chip } from '@/components/Chip'
import { Painel } from '@/components/Painel'
import { useToast } from '@/hooks/useToast'
import { useSalvarVinculos, useUsuariosPorPerfil, useVinculos } from '@/hooks/useUsuarios'
import { cn } from '@/lib/utils'

export default function Vinculos() {
  const { avisar } = useToast()
  const supervisores = useUsuariosPorPerfil('SUPERVISOR')
  const estagiarios = useUsuariosPorPerfil('ESTAGIARIO')
  const salvar = useSalvarVinculos()

  const [supervisorId, setSupervisorId] = useState('')
  const [selecionados, setSelecionados] = useState<number[]>([])

  // O PUT substitui o conjunto inteiro, então a tela precisa abrir marcada com o
  // que já existe — salvar sem isto apagaria em silêncio os vínculos atuais.
  const vinculos = useVinculos(supervisorId ? Number(supervisorId) : undefined)

  useEffect(() => {
    const primeiro = supervisores.data?.content[0]
    if (primeiro && !supervisorId) setSupervisorId(String(primeiro.id))
  }, [supervisores.data, supervisorId])

  useEffect(() => {
    if (!vinculos.data) return
    setSelecionados(vinculos.data.map((estagiario) => estagiario.id))
  }, [vinculos.data])

  function alternar(id: number) {
    setSelecionados((atuais) =>
      atuais.includes(id) ? atuais.filter((outro) => outro !== id) : [...atuais, id],
    )
  }

  async function enviar() {
    if (!supervisorId) return
    await salvar.mutateAsync({
      supervisorId: Number(supervisorId),
      estagiarioIds: selecionados,
    })
    avisar('Vínculos salvos', `${selecionados.length} estagiário(s) podem escolher este supervisor.`)
  }

  const supervisor = supervisores.data?.content.find(
    (usuario) => String(usuario.id) === supervisorId,
  )

  const erroDeLeitura = supervisores.error ?? estagiarios.error ?? vinculos.error

  return (
    <>
      <CabecalhoPagina
        titulo="Vínculos supervisor · estagiários"
        subtitulo="Define quem pode ser escolhido como supervisor ao abrir um atendimento."
      />

      {erroDeLeitura ? <BannerErro erro={erroDeLeitura} /> : null}
      {salvar.isError ? <BannerErro erro={salvar.error} /> : null}

      <div className="flex max-w-[820px] flex-col gap-4">
        <Painel>
          <h2 className="mb-1 text-3xl font-bold tracking-snug">Supervisor e seus estagiários</h2>
          <p className="text-ink-muted mb-4 text-base leading-normal">
            Só estagiários vinculados podem escolher este supervisor ao abrir um atendimento.
          </p>

          <label className="mb-[18px] block max-w-[340px]">
            <span className="text-ink-label mb-1.5 block text-base font-semibold">Supervisor</span>
            <select
              value={supervisorId}
              onChange={(evento) => {
                setSupervisorId(evento.target.value)
                // Zera enquanto o vínculo do novo supervisor não chega, para não
                // exibir por um instante a marcação do supervisor anterior.
                setSelecionados([])
              }}
              className="text-ink bg-surface border-line h-10 w-full cursor-pointer appearance-none rounded-lg border pr-8 pl-3 text-lg outline-none"
            >
              <option value="">Selecione…</option>
              {(supervisores.data?.content ?? []).map((usuario) => (
                <option key={usuario.id} value={usuario.id}>
                  {usuario.nome}
                </option>
              ))}
            </select>
          </label>

          <div className="flex flex-col gap-2">
            {(estagiarios.data?.content ?? []).map((estagiario) => {
              const marcado = selecionados.includes(estagiario.id)
              return (
                <button
                  key={estagiario.id}
                  type="button"
                  onClick={() => alternar(estagiario.id)}
                  aria-pressed={marcado}
                  disabled={!supervisorId}
                  className={cn(
                    'flex w-full items-center gap-3 rounded-xl border p-3 text-left',
                    marcado ? 'bg-brand-tint border-brand-border' : 'bg-surface border-line-card',
                    !supervisorId && 'cursor-not-allowed opacity-60',
                  )}
                >
                  <span
                    className={cn(
                      'inline-flex size-5 shrink-0 items-center justify-center rounded-xs border-[1.5px]',
                      marcado ? 'bg-brand border-brand' : 'bg-surface border-control-off',
                    )}
                  >
                    {marcado ? (
                      <span className="-mt-0.5 h-[5px] w-[9px] -rotate-45 border-b-2 border-l-2 border-white" />
                    ) : null}
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="block text-lg font-semibold">{estagiario.nome}</span>
                    <span className="text-ink-muted mt-px block text-base">
                      {estagiario.email}
                    </span>
                  </span>
                  {!estagiario.ativo ? <Chip tom="neutro">inativo</Chip> : null}
                </button>
              )
            })}

            {!estagiarios.isPending && (estagiarios.data?.content.length ?? 0) === 0 ? (
              <p className="bg-surface-sunken border-line text-ink-muted rounded-2xl border border-dashed p-5 text-center text-md">
                Nenhum estagiário cadastrado.
              </p>
            ) : null}
          </div>
        </Painel>

        <div className="flex flex-wrap items-center justify-between gap-3">
          <span className="text-ink-muted text-base">
            {selecionados.length} estagiário(s) vinculado(s)
            {supervisor ? ` a ${supervisor.nome}` : ''}
          </span>
          <Botao
            type="button"
            tamanho="lg"
            disabled={!supervisorId || salvar.isPending || vinculos.isPending}
            onClick={() => void enviar()}
          >
            {salvar.isPending ? 'Salvando…' : 'Salvar vínculos'}
          </Botao>
        </div>
      </div>
    </>
  )
}
