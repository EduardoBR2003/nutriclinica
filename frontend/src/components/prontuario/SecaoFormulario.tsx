import { useEffect, type ReactNode } from 'react'
import { useForm, type FieldValues, type Path, type UseFormReturn } from 'react-hook-form'

import { BannerErro } from '@/components/BannerErro'
import { GradeCampos } from '@/components/campos/GradeCampos'
import type { CampoDef } from '@/components/campos/tipos'
import { CartaoSecao } from '@/components/prontuario/CartaoSecao'
import { RodapeSalvar } from '@/components/prontuario/RodapeSalvar'
import { useSalvarSecao } from '@/hooks/useSecoes'
import { useToast } from '@/hooks/useToast'
import { aplicarErrosDeCampo } from '@/lib/erros'
import { paraFormulario, paraPatch } from '@/lib/patch'
import type { SecaoProntuario } from '@/types/dominio'

interface SecaoFormularioProps<T extends FieldValues, Res> {
  atendimentoId: number
  secao: SecaoProntuario
  titulo: string
  descricao: string
  preenchida: boolean
  campos: readonly CampoDef<T>[]
  padroes: T
  /** Valores atuais vindos do `AtendimentoCompleto`. */
  valores: Partial<T> | undefined
  editavel: boolean
  /** Conteúdo extra abaixo da grade (indicadores calculados, sentença PES…). */
  extra?: (form: UseFormReturn<T>, resposta: Res | undefined) => ReactNode
}

/** As sete seções 1:1 do contrato — salvamento por `PATCH` parcial. */
export function SecaoFormulario<T extends FieldValues, Res = unknown>({
  atendimentoId,
  secao,
  titulo,
  descricao,
  preenchida,
  campos,
  padroes,
  valores,
  editavel,
  extra,
}: SecaoFormularioProps<T, Res>) {
  const { avisar } = useToast()
  const salvar = useSalvarSecao<T, Res>(atendimentoId, secao, 'patch')

  const form = useForm<T>({ defaultValues: paraFormulario(valores as T, padroes) as never })
  const { reset } = form

  useEffect(() => {
    reset(paraFormulario(valores as T, padroes) as never)
  }, [valores, padroes, reset, secao])

  const aoEnviar = form.handleSubmit(async (dados) => {
    try {
      await salvar.mutateAsync(paraPatch(dados as Record<string, unknown>) as T)
      avisar('Seção salva', titulo)
    } catch (erro) {
      aplicarErrosDeCampo<T>(
        erro,
        form.setError,
        campos.map((campo) => campo.nome as Path<T>),
      )
    }
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
        <GradeCampos form={form} campos={campos} travado={!editavel} />
        {salvar.isError ? (
          <div className="mt-4">
            <BannerErro erro={salvar.error} />
          </div>
        ) : null}
        {extra?.(form, salvar.data)}
      </CartaoSecao>
    </form>
  )
}
