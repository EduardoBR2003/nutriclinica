import { useEffect } from 'react'
import { useFieldArray, useForm } from 'react-hook-form'

import { BannerErro } from '@/components/BannerErro'
import { Botao } from '@/components/Botao'
import { Chip } from '@/components/Chip'
import { GradeCampos } from '@/components/campos/GradeCampos'
import type { CampoDef } from '@/components/campos/tipos'
import { useAvaliacao, useRegistrarAvaliacao } from '@/hooks/useRevisoes'
import { useToast } from '@/hooks/useToast'
import { formatarDataHora, formatarNumero } from '@/lib/formato'
import type { AvaliacaoRequest, ResultadoAvaliacao } from '@/types/dominio'

interface ItemRubrica {
  criterio: string
  peso: number | null
  nota: number | null
  comentario: string
}

interface FormRubrica {
  parecerGeral: string
  itens: ItemRubrica[]
}

const PADRAO_ITEM: ItemRubrica = { criterio: '', peso: 1, nota: null, comentario: '' }

const CAMPOS_ITEM: readonly CampoDef<ItemRubrica>[] = [
  { nome: 'criterio', label: 'Critério', tipo: 'texto', obrigatorio: true, largo: true },
  { nome: 'peso', label: 'Peso', tipo: 'numero', obrigatorio: true },
  { nome: 'nota', label: 'Nota (0–10)', tipo: 'numero', obrigatorio: true },
  { nome: 'comentario', label: 'Comentário', tipo: 'area', largo: true },
]

interface PainelAvaliacaoProps {
  atendimentoId: number
  /** Supervisor designado com o atendimento em EM_REVISAO. */
  podeAvaliar: boolean
  avaliadoEm: string | undefined
}

export function PainelAvaliacao({ atendimentoId, podeAvaliar, avaliadoEm }: PainelAvaliacaoProps) {
  const { avisar } = useToast()
  const consulta = useAvaliacao(atendimentoId, Boolean(avaliadoEm) || !podeAvaliar)
  const registrar = useRegistrarAvaliacao(atendimentoId)

  const form = useForm<FormRubrica>({
    defaultValues: { parecerGeral: '', itens: [PADRAO_ITEM] },
  })
  const itens = useFieldArray({ control: form.control, name: 'itens' })
  const { reset } = form

  // Reavaliar reaproveita a linha e troca os itens da rubrica: partir do que já
  // foi registrado poupa o supervisor de redigitar a rubrica inteira.
  useEffect(() => {
    const anterior = consulta.data
    if (!anterior?.itens?.length) return
    reset({
      parecerGeral: anterior.parecerGeral ?? '',
      itens: anterior.itens.map((item) => ({
        criterio: item.criterio ?? '',
        peso: item.peso ?? 1,
        nota: item.nota ?? null,
        comentario: item.comentario ?? '',
      })),
    })
  }, [consulta.data, reset])

  // Prévia local só para orientar o supervisor enquanto digita; a nota que
  // vale é a que o servidor calcula — `AvaliacaoRequest` nem declara notaFinal.
  const observados = form.watch('itens')
  const validos = observados.filter(
    (item) => typeof item.nota === 'number' && typeof item.peso === 'number' && item.peso !== 0,
  )
  const somaPesos = validos.reduce((total, item) => total + (item.peso ?? 0), 0)
  const previa = somaPesos
    ? validos.reduce((total, item) => total + (item.nota ?? 0) * (item.peso ?? 0), 0) / somaPesos
    : null

  async function registrarCom(resultado: ResultadoAvaliacao) {
    const valores = form.getValues()
    const corpo: AvaliacaoRequest = {
      resultado,
      parecerGeral: valores.parecerGeral || undefined,
      itens: valores.itens
        .filter((item) => item.criterio.trim() && typeof item.nota === 'number')
        .map((item) => ({
          criterio: item.criterio.trim(),
          peso: item.peso ?? 1,
          nota: item.nota as number,
          comentario: item.comentario || undefined,
        })),
    }
    if (corpo.itens.length === 0) {
      form.setError('itens', {
        type: 'manual',
        message: 'Informe ao menos um critério com nota.',
      })
      return
    }
    const avaliacao = await registrar.mutateAsync(corpo)
    avisar(
      resultado === 'APROVADO' ? 'Aprovado' : 'Devolvido para correção',
      `Nota final ${formatarNumero(avaliacao.notaFinal, 2)} calculada pelo servidor.`,
    )
  }

  const registrada = consulta.data

  return (
    <section className="bg-surface border-line-card shadow-panel overflow-hidden rounded-4xl border">
      <div className="flex flex-wrap items-center justify-between gap-3 px-[18px] pt-4 pb-3">
        <div>
          <h2 className="text-2xl font-bold tracking-snug">
            {podeAvaliar ? 'Avaliação por rubrica' : 'Parecer do supervisor'}
          </h2>
          <p className="text-ink-muted mt-0.5 text-base">
            {podeAvaliar
              ? 'A nota final é média ponderada dos critérios, calculada pelo servidor.'
              : avaliadoEm
                ? `Avaliado em ${formatarDataHora(avaliadoEm)}`
                : 'Aguardando avaliação'}
          </p>
        </div>
        {/* Enquanto o supervisor edita, o número mostrado é prévia local — a
            nota que vale é a que o servidor devolve no POST. */}
        {podeAvaliar ? (
          previa !== null ? <Chip tom="neutro">prévia {formatarNumero(previa, 2)}/10</Chip> : null
        ) : registrada?.notaFinal !== undefined ? (
          <Chip tom={registrada.notaFinal >= 7 ? 'sucesso' : 'alerta'}>
            nota final {formatarNumero(registrada.notaFinal, 2)}/10
          </Chip>
        ) : null}
      </div>

      {podeAvaliar ? (
        <div className="flex flex-col gap-3.5 px-[18px] pb-[18px]">
          {registrar.isError ? <BannerErro erro={registrar.error} /> : null}

          <div className="flex flex-col gap-2.5">
            {itens.fields.map((campo, indice) => (
              <div
                key={campo.id}
                className="bg-surface-sunken border-line-subtle rounded-2xl border p-3.5"
              >
                <div className="mb-2.5 flex items-center justify-between gap-2.5">
                  <span className="text-ink-label text-base font-bold">
                    {form.watch(`itens.${indice}.criterio`) || `Critério ${indice + 1}`}
                  </span>
                  <button
                    type="button"
                    onClick={() => itens.remove(indice)}
                    className="text-ink-soft border-line-card h-6.5 rounded-sm border bg-white px-2.5 text-sm font-semibold"
                  >
                    Remover
                  </button>
                </div>
                <GradeCampos
                  form={form as never}
                  campos={
                    CAMPOS_ITEM.map((definicao) => ({
                      ...definicao,
                      nome: `itens.${indice}.${String(definicao.nome)}`,
                    })) as never
                  }
                />
              </div>
            ))}
            {form.formState.errors.itens?.message ? (
              <p className="text-cta-text text-sm font-semibold">
                {form.formState.errors.itens.message}
              </p>
            ) : null}
            <Botao
              type="button"
              variante="suave"
              className="border-dashed"
              onClick={() => itens.append(PADRAO_ITEM)}
            >
              + Critério da rubrica
            </Botao>
          </div>

          <div>
            <label
              htmlFor="parecer-geral"
              className="text-ink-label mb-2 block text-base font-semibold"
            >
              Parecer geral
            </label>
            <textarea
              id="parecer-geral"
              rows={4}
              {...form.register('parecerGeral')}
              placeholder="Justifique a decisão e aponte o que sustenta a nota."
              className="text-ink bg-surface border-line focus:border-brand-muted focus:ring-brand-tint-hover w-full resize-y rounded-xl border px-3 py-2.5 text-md leading-relaxed outline-none focus:ring-3"
            />
          </div>

          <div className="grid grid-cols-2 gap-2.5">
            <Botao
              type="button"
              tamanho="lg"
              disabled={registrar.isPending}
              onClick={() => void registrarCom('APROVADO')}
            >
              Aprovar
            </Botao>
            <Botao
              type="button"
              variante="cta"
              tamanho="lg"
              disabled={registrar.isPending}
              onClick={() => void registrarCom('DEVOLVIDO')}
            >
              Devolver para correção
            </Botao>
          </div>
        </div>
      ) : (
        <div className="px-[18px] pb-[18px]">
          <p className="text-ink-body text-md leading-loose">
            {registrada?.parecerGeral || 'Nenhum parecer registrado ainda.'}
          </p>
          <div className="mt-3.5 flex flex-col gap-2">
            {(registrada?.itens ?? []).map((item) => (
              <div
                key={item.id ?? item.criterio}
                className="border-line-faint flex items-baseline justify-between gap-3 border-b pb-2"
              >
                <span className="text-ink-soft min-w-0 text-base">{item.criterio}</span>
                <span className="text-ink-body text-base font-semibold whitespace-nowrap">
                  nota {formatarNumero(item.nota, 1)} · peso {formatarNumero(item.peso, 1)}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </section>
  )
}
