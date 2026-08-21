import { useEffect } from 'react'
import { useFieldArray, useForm, type ArrayPath, type FieldArray, type FieldValues } from 'react-hook-form'

import { BannerErro } from '@/components/BannerErro'
import { Botao } from '@/components/Botao'
import { GradeCampos } from '@/components/campos/GradeCampos'
import type { CampoDef } from '@/components/campos/tipos'
import { CartaoSecao } from '@/components/prontuario/CartaoSecao'
import { RodapeSalvar } from '@/components/prontuario/RodapeSalvar'
import { useSalvarSecao } from '@/hooks/useSecoes'
import { useToast } from '@/hooks/useToast'
import { paraPatch } from '@/lib/patch'
import type { SecaoProntuario } from '@/types/dominio'

interface Envelope<Item> extends FieldValues {
  itens: Item[]
}

interface SecaoListaProps<Item extends FieldValues> {
  atendimentoId: number
  secao: SecaoProntuario
  titulo: string
  descricao: string
  preenchida: boolean
  rotuloItem: string
  textoVazio: string
  campos: readonly CampoDef<Item>[]
  padraoItem: Item
  valores: Item[] | undefined
  editavel: boolean
}

/**
 * Medicamentos, exames e metas: `PUT` que substitui a coleção inteira.
 * O `id` de quem continuou vai junto, e é isso que faz o backend preservar a
 * linha em vez de recriá-la (`orphanRemoval` reconcilia pelo id).
 */
export function SecaoLista<Item extends FieldValues>({
  atendimentoId,
  secao,
  titulo,
  descricao,
  preenchida,
  rotuloItem,
  textoVazio,
  campos,
  padraoItem,
  valores,
  editavel,
}: SecaoListaProps<Item>) {
  const { avisar } = useToast()
  const salvar = useSalvarSecao<Item[], Item[]>(atendimentoId, secao, 'put')

  const form = useForm<Envelope<Item>>({
    defaultValues: { itens: valores ?? [] } as never,
  })
  const { reset, control } = form
  const lista = useFieldArray<Envelope<Item>>({
    control,
    name: 'itens' as ArrayPath<Envelope<Item>>,
  })

  useEffect(() => {
    reset({ itens: valores ?? [] } as never)
  }, [valores, reset, secao])

  const aoEnviar = form.handleSubmit(async (dados) => {
    const itens = dados.itens.map(
      (item) => paraPatch(item as Record<string, unknown>) as unknown as Item,
    )
    await salvar.mutateAsync(itens)
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
          {lista.fields.map((campo, indice) => (
            <div
              key={campo.id}
              className="bg-surface-sunken border-line-subtle rounded-2xl border p-4"
            >
              <div className="mb-3 flex items-center justify-between gap-2.5">
                <span className="text-ink-soft text-base font-bold tracking-wide uppercase">
                  {rotuloItem} {indice + 1}
                </span>
                {editavel ? (
                  <button
                    type="button"
                    onClick={() => lista.remove(indice)}
                    className="text-cta-text border-cta-border h-7 rounded-sm border bg-white px-2.5 text-sm font-semibold"
                  >
                    Remover
                  </button>
                ) : null}
              </div>
              <GradeCampos
                // Os campos do item são reindexados para `itens.N.campo`.
                form={form as never}
                campos={campos.map((definicao) => ({
                  ...definicao,
                  nome: `itens.${indice}.${String(definicao.nome)}`,
                })) as never}
                travado={!editavel}
              />
            </div>
          ))}

          {lista.fields.length === 0 ? (
            <p className="bg-surface-sunken border-line text-ink-muted rounded-2xl border border-dashed p-5 text-center text-md">
              {textoVazio}
            </p>
          ) : null}

          {editavel ? (
            <Botao
              type="button"
              variante="suave"
              tamanho="md"
              className="border-dashed"
              onClick={() =>
                lista.append(padraoItem as unknown as FieldArray<Envelope<Item>, ArrayPath<Envelope<Item>>>)
              }
            >
              + {rotuloItem}
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
