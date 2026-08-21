import { Controller, type FieldValues, type UseFormReturn } from 'react-hook-form'

import type { CampoDef } from '@/components/campos/tipos'
import { cn } from '@/lib/utils'

const BASE = 'w-full text-lg rounded-lg outline-none transition-colors'
const FOCO =
  'focus:border-brand-muted focus:ring-3 focus:ring-brand-tint-hover focus-visible:border-brand-muted'

function cores(travado: boolean, comErro: boolean) {
  if (travado) return 'text-ink-muted bg-disabled border border-line-card cursor-not-allowed'
  return cn('text-ink bg-surface border', comErro ? 'border-cta' : 'border-line')
}

interface GradeCamposProps<T extends FieldValues> {
  form: UseFormReturn<T>
  campos: readonly CampoDef<T>[]
  /** Somente leitura para todos os campos — é assim que `editavel: false` aparece. */
  travado?: boolean
}

/**
 * Grade responsiva de campos do design: colunas de no mínimo 190px que se
 * ajustam sozinhas, com os campos `largo` ocupando a linha inteira.
 */
export function GradeCampos<T extends FieldValues>({
  form,
  campos,
  travado = false,
}: GradeCamposProps<T>) {
  const { register, control, formState } = form

  return (
    <div className="grid grid-cols-[repeat(auto-fit,minmax(min(100%,190px),1fr))] gap-3.5">
      {campos.map((campo) => {
        const erro = formState.errors[campo.nome]
        const mensagemErro = typeof erro?.message === 'string' ? erro.message : ''
        const bloqueado = travado || campo.somenteLeitura !== undefined
        const classe = cn(BASE, cores(bloqueado, Boolean(mensagemErro)), !bloqueado && FOCO)

        return (
          <div key={campo.nome} className={cn('min-w-0', campo.largo && 'col-span-full')}>
            <label
              htmlFor={campo.nome}
              className="mb-1.5 flex items-baseline gap-1.5 leading-snug"
            >
              <span className="text-ink-label text-base font-semibold">{campo.label}</span>
              {campo.obrigatorio ? (
                <span className="text-cta text-base font-bold" aria-hidden>
                  *
                </span>
              ) : null}
              {campo.somenteLeitura ? (
                <span className="text-ink-muted text-2xs font-semibold tracking-wide uppercase">
                  {campo.somenteLeitura === 'calculado' ? 'calculado' : 'somente leitura'}
                </span>
              ) : null}
            </label>

            {campo.tipo === 'bool' ? (
              <Controller
                control={control}
                name={campo.nome}
                render={({ field }) => (
                  <button
                    type="button"
                    id={campo.nome}
                    disabled={bloqueado}
                    onClick={() => field.onChange(!field.value)}
                    className={cn(
                      'flex h-10 w-full items-center gap-2.5 rounded-lg px-3 text-left',
                      bloqueado
                        ? 'text-ink-muted bg-disabled border-line-card cursor-not-allowed border'
                        : field.value
                          ? 'text-brand-text bg-brand-tint border-brand-border cursor-pointer border'
                          : 'text-ink-soft bg-surface border-line cursor-pointer border',
                    )}
                  >
                    <span
                      className={cn(
                        'inline-flex h-[18px] w-8 shrink-0 items-center rounded-full p-0.5 transition-colors',
                        field.value ? 'bg-brand' : 'bg-control-off',
                      )}
                    >
                      <span
                        className={cn(
                          'size-3.5 rounded-full bg-white transition-transform',
                          field.value && 'translate-x-3.5',
                        )}
                      />
                    </span>
                    <span className="text-md font-medium">{field.value ? 'Sim' : 'Não'}</span>
                  </button>
                )}
              />
            ) : campo.tipo === 'select' ? (
              <div className="relative">
                <select
                  id={campo.nome}
                  disabled={bloqueado}
                  aria-invalid={Boolean(mensagemErro)}
                  {...register(campo.nome)}
                  className={cn(classe, 'h-10 cursor-pointer appearance-none pr-8 pl-3')}
                >
                  {campo.opcoes?.map((opcao) => (
                    <option key={opcao.valor} value={opcao.valor}>
                      {opcao.texto}
                    </option>
                  ))}
                </select>
                <span
                  aria-hidden
                  className="border-ink-soft pointer-events-none absolute top-[15px] right-3 size-[7px] rotate-45 border-r-[1.5px] border-b-[1.5px]"
                />
              </div>
            ) : campo.tipo === 'area' ? (
              <textarea
                id={campo.nome}
                rows={campo.linhas ?? 3}
                disabled={bloqueado}
                placeholder={campo.dica}
                aria-invalid={Boolean(mensagemErro)}
                {...register(campo.nome)}
                className={cn(classe, 'resize-y px-3 py-2.5 leading-relaxed')}
              />
            ) : campo.tipo === 'numero' ? (
              <div className="relative">
                <input
                  id={campo.nome}
                  type="number"
                  step="any"
                  disabled={bloqueado}
                  placeholder={campo.dica}
                  aria-invalid={Boolean(mensagemErro)}
                  {...register(campo.nome, {
                    // Campo vazio precisa virar `null` — no PATCH do contrato é
                    // `null` que limpa; ausente manteria o valor antigo.
                    setValueAs: (valor) =>
                      valor === '' || valor === null || valor === undefined
                        ? null
                        : Number(valor),
                  })}
                  className={cn(classe, 'h-10 pl-3', campo.unidade ? 'pr-11' : 'pr-3')}
                />
                {campo.unidade ? (
                  <span className="text-ink-muted pointer-events-none absolute top-3 right-3 text-sm font-medium">
                    {campo.unidade}
                  </span>
                ) : null}
              </div>
            ) : (
              <input
                id={campo.nome}
                type={
                  campo.tipo === 'senha' ? 'password' : campo.tipo === 'data' ? 'date' : 'text'
                }
                disabled={bloqueado}
                placeholder={campo.dica}
                aria-invalid={Boolean(mensagemErro)}
                {...register(campo.nome)}
                className={cn(classe, 'h-10 px-3')}
              />
            )}

            {mensagemErro ? (
              <p className="text-cta-text mt-1.5 text-sm font-semibold">{mensagemErro}</p>
            ) : campo.ajuda ? (
              <p className="text-ink-muted mt-1.5 text-xs">{campo.ajuda}</p>
            ) : null}
          </div>
        )
      })}
    </div>
  )
}
