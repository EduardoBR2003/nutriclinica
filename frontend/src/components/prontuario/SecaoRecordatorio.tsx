import { useEffect } from 'react'
import { useFieldArray, useForm } from 'react-hook-form'

import { BannerErro } from '@/components/BannerErro'
import { Botao } from '@/components/Botao'
import { GradeCampos } from '@/components/campos/GradeCampos'
import { CartaoSecao } from '@/components/prontuario/CartaoSecao'
import { RodapeSalvar } from '@/components/prontuario/RodapeSalvar'
import {
  CAMPOS_ITEM_REFEICAO,
  CAMPOS_REFEICAO,
  PADRAO_ITEM_REFEICAO,
  PADRAO_REFEICAO,
  type FormRefeicao,
} from '@/components/prontuario/secoes'
import { useSalvarSecao } from '@/hooks/useSecoes'
import { useToast } from '@/hooks/useToast'
import { paraPatch } from '@/lib/patch'
import type { Refeicao } from '@/types/dominio'

interface Envelope {
  refeicoes: FormRefeicao[]
}

interface SecaoRecordatorioProps {
  atendimentoId: number
  titulo: string
  descricao: string
  preenchida: boolean
  valores: Refeicao[] | undefined
  editavel: boolean
}

/** Única seção com coleção aninhada: refeições e, dentro delas, os alimentos. */
export function SecaoRecordatorio({
  atendimentoId,
  titulo,
  descricao,
  preenchida,
  valores,
  editavel,
}: SecaoRecordatorioProps) {
  const { avisar } = useToast()
  const salvar = useSalvarSecao<Refeicao[], Refeicao[]>(atendimentoId, 'RECORDATORIO', 'put')

  const form = useForm<Envelope>({ defaultValues: { refeicoes: [] } })
  const { reset, control } = form
  const refeicoes = useFieldArray({ control, name: 'refeicoes' })

  useEffect(() => {
    reset({
      refeicoes: (valores ?? []).map((refeicao) => ({
        id: refeicao.id,
        tipoRefeicao: refeicao.tipoRefeicao,
        horario: refeicao.horario ?? '',
        localRefeicao: refeicao.localRefeicao ?? '',
        itens: (refeicao.itens ?? []).map((item) => ({
          id: item.id,
          alimento: item.alimento,
          quantidade: item.quantidade ?? '',
          medidaCaseira: item.medidaCaseira ?? '',
        })),
      })),
    })
  }, [valores, reset])

  const aoEnviar = form.handleSubmit(async (dados) => {
    // `ordem` é responsabilidade do cliente: é a sequência das refeições no dia.
    const corpo = dados.refeicoes.map((refeicao, indice) => ({
      ...(paraPatch(refeicao as unknown as Record<string, unknown>) as unknown as FormRefeicao),
      ordem: indice,
      itens: refeicao.itens.map((item, posicao) => ({
        ...(paraPatch(item as unknown as Record<string, unknown>) as unknown as object),
        ordem: posicao,
      })),
    })) as unknown as Refeicao[]
    await salvar.mutateAsync(corpo)
    avisar('Seção salva', titulo)
  })

  return (
    <form onSubmit={aoEnviar} noValidate>
      <CartaoSecao
        titulo={titulo}
        descricao={descricao}
        preenchida={preenchida}
        rodape={
          <RodapeSalvar
            visivel={editavel}
            salvando={salvar.isPending}
            sujo={form.formState.isDirty}
          />
        }
      >
        <div className="flex flex-col gap-3">
          {refeicoes.fields.map((refeicao, indice) => (
            <RefeicaoEditor
              key={refeicao.id}
              form={form}
              indice={indice}
              editavel={editavel}
              aoRemover={() => refeicoes.remove(indice)}
            />
          ))}

          {refeicoes.fields.length === 0 ? (
            <p className="bg-surface-sunken border-line text-ink-muted rounded-2xl border border-dashed p-5 text-center text-md">
              Nenhuma refeição registrada — obrigatório ao menos uma para submeter.
            </p>
          ) : null}

          {editavel ? (
            <Botao
              type="button"
              variante="suave"
              className="border-dashed"
              onClick={() => refeicoes.append(PADRAO_REFEICAO)}
            >
              + Refeição
            </Botao>
          ) : null}
        </div>

        {salvar.isError ? (
          <div className="mt-4">
            <BannerErro erro={salvar.error} />
          </div>
        ) : null}
      </CartaoSecao>
    </form>
  )
}

interface RefeicaoEditorProps {
  form: ReturnType<typeof useForm<Envelope>>
  indice: number
  editavel: boolean
  aoRemover: () => void
}

function RefeicaoEditor({ form, indice, editavel, aoRemover }: RefeicaoEditorProps) {
  const itens = useFieldArray({ control: form.control, name: `refeicoes.${indice}.itens` })

  return (
    <div className="bg-surface-sunken border-line-subtle rounded-2xl border p-4">
      <div className="mb-3 flex items-center justify-between gap-2.5">
        <span className="text-ink-soft text-base font-bold tracking-wide uppercase">
          Refeição {indice + 1}
        </span>
        {editavel ? (
          <button
            type="button"
            onClick={aoRemover}
            className="text-cta-text border-cta-border h-7 rounded-sm border bg-white px-2.5 text-sm font-semibold"
          >
            Remover
          </button>
        ) : null}
      </div>

      <GradeCampos
        form={form as never}
        campos={
          CAMPOS_REFEICAO.map((definicao) => ({
            ...definicao,
            nome: `refeicoes.${indice}.${String(definicao.nome)}`,
          })) as never
        }
        travado={!editavel}
      />

      <div className="border-line mt-3.5 flex flex-col gap-2.5 border-t border-dashed pt-3.5">
        <span className="text-ink-muted text-sm font-semibold tracking-wide uppercase">
          Alimentos da refeição
        </span>

        {itens.fields.map((item, posicao) => (
          <div
            key={item.id}
            className="bg-surface border-line-card flex items-start gap-2.5 rounded-xl border p-3"
          >
            <div className="min-w-0 flex-1">
              <GradeCampos
                form={form as never}
                campos={
                  CAMPOS_ITEM_REFEICAO.map((definicao) => ({
                    ...definicao,
                    nome: `refeicoes.${indice}.itens.${posicao}.${String(definicao.nome)}`,
                  })) as never
                }
                travado={!editavel}
              />
            </div>
            {editavel ? (
              <button
                type="button"
                onClick={() => itens.remove(posicao)}
                aria-label="Remover alimento"
                className="text-ink-muted border-line mt-6 inline-flex size-7 shrink-0 items-center justify-center rounded-sm border bg-white text-2xl"
              >
                ×
              </button>
            ) : null}
          </div>
        ))}

        {editavel ? (
          <Botao
            type="button"
            variante="contorno"
            tamanho="sm"
            className="border-brand-border text-brand-text border-dashed"
            onClick={() => itens.append(PADRAO_ITEM_REFEICAO)}
          >
            + Alimento
          </Botao>
        ) : null}
      </div>
    </div>
  )
}
