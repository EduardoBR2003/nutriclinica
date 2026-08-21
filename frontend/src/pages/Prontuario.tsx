import { useState } from 'react'
import { useParams } from 'react-router'

import { BannerErro } from '@/components/BannerErro'
import { Painel } from '@/components/Painel'
import { AbasSecoes } from '@/components/prontuario/AbasSecoes'
import { CabecalhoProntuario } from '@/components/prontuario/CabecalhoProntuario'
import { PainelAvaliacao } from '@/components/prontuario/PainelAvaliacao'
import { PainelCalculados } from '@/components/prontuario/PainelCalculados'
import { PainelComentarios } from '@/components/prontuario/PainelComentarios'
import { SecaoFormulario } from '@/components/prontuario/SecaoFormulario'
import { SecaoLista } from '@/components/prontuario/SecaoLista'
import { SecaoRecordatorio } from '@/components/prontuario/SecaoRecordatorio'
import {
  CAMPOS_ANTROPOMETRIA,
  CAMPOS_COMPORTAMENTO,
  CAMPOS_DIAGNOSTICO,
  CAMPOS_EXAME,
  CAMPOS_FREQUENCIA,
  CAMPOS_HISTORIA,
  CAMPOS_MEDICAMENTO,
  CAMPOS_META,
  CAMPOS_PLANO,
  CAMPOS_QUEIXA,
  META_SECAO,
  PADRAO_ANTROPOMETRIA,
  PADRAO_COMPORTAMENTO,
  PADRAO_DIAGNOSTICO,
  PADRAO_EXAME,
  PADRAO_FREQUENCIA,
  PADRAO_HISTORIA,
  PADRAO_MEDICAMENTO,
  PADRAO_META,
  PADRAO_PLANO,
  PADRAO_QUEIXA,
  ROTULO_ITEM,
  type FormAntropometria,
  type FormComportamento,
  type FormDiagnostico,
  type FormExame,
  type FormFrequencia,
  type FormHistoria,
  type FormMedicamento,
  type FormMeta,
  type FormPlano,
  type FormQueixa,
} from '@/components/prontuario/secoes'
import { useAtendimento, useSubmeterAtendimento } from '@/hooks/useAtendimentos'
import { useAuth } from '@/hooks/useAuth'
import { useComentarios } from '@/hooks/useRevisoes'
import { useToast } from '@/hooks/useToast'
import type {
  Antropometria,
  AtendimentoCompleto,
  SecaoProntuario,
} from '@/types/dominio'

export default function Prontuario() {
  const { id } = useParams()
  const atendimentoId = Number(id)
  const { usuario } = useAuth()
  const { avisar } = useToast()

  const [secao, setSecao] = useState<SecaoProntuario>('QUEIXA_PRINCIPAL')
  const [erroSubmissao, setErroSubmissao] = useState<unknown>(null)

  const consulta = useAtendimento(atendimentoId)
  const submeter = useSubmeterAtendimento(atendimentoId)
  const comentarios = useComentarios(atendimentoId)

  const atendimento = consulta.data

  if (consulta.isPending) {
    return (
      <Painel>
        <p className="text-ink-muted text-lg">Carregando prontuário…</p>
      </Painel>
    )
  }

  if (consulta.isError || !atendimento) {
    return <BannerErro erro={consulta.error} />
  }

  const meta = META_SECAO[secao]
  const preenchida = atendimento.secoesPreenchidas.includes(secao)
  const editavel = atendimento.editavel

  const podeAvaliar =
    usuario?.perfil === 'SUPERVISOR' &&
    atendimento.status === 'EM_REVISAO' &&
    atendimento.supervisor.id === usuario.id
  // O parecer não fecha junto com o prontuário: o supervisor designado comenta
  // em qualquer status.
  const podeComentar =
    usuario?.perfil === 'SUPERVISOR' && atendimento.supervisor.id === usuario.id

  const pendencias: Partial<Record<SecaoProntuario, number>> = {}
  for (const comentario of comentarios.data ?? []) {
    if (comentario.resolvido || !comentario.secao) continue
    pendencias[comentario.secao] = (pendencias[comentario.secao] ?? 0) + 1
  }

  async function aoSubmeter() {
    setErroSubmissao(null)
    try {
      await submeter.mutateAsync()
      avisar('Enviado para revisão', 'O supervisor recebe o prontuário na fila de correção.')
    } catch (erro) {
      // 409 SECOES_INCOMPLETAS traz um item de `campos` por campo pendente.
      setErroSubmissao(erro)
    }
  }

  return (
    <>
      <CabecalhoProntuario
        atendimento={atendimento}
        submetendo={submeter.isPending}
        aoSubmeter={() => void aoSubmeter()}
      />

      {erroSubmissao ? (
        <BannerErro erro={erroSubmissao} aoFechar={() => setErroSubmissao(null)} />
      ) : null}

      <AbasSecoes
        atual={secao}
        preenchidas={atendimento.secoesPreenchidas}
        pendencias={pendencias}
        aoTrocar={setSecao}
      />

      <div className="grid grid-cols-[repeat(auto-fit,minmax(min(100%,430px),1fr))] items-start gap-4">
        <div className="flex min-w-0 flex-col gap-4">
          <PainelSecao
            key={secao}
            secao={secao}
            atendimento={atendimento}
            editavel={editavel}
            preenchida={preenchida}
          />
        </div>

        <div className="flex min-w-0 flex-col gap-4">
          <PainelComentarios
            atendimentoId={atendimentoId}
            secao={secao}
            tituloSecao={meta.titulo}
            podeComentar={podeComentar}
          />
          <PainelAvaliacao
            atendimentoId={atendimentoId}
            podeAvaliar={podeAvaliar}
            avaliadoEm={atendimento.avaliadoEm}
          />
        </div>
      </div>
    </>
  )
}

interface PainelSecaoProps {
  secao: SecaoProntuario
  atendimento: AtendimentoCompleto
  editavel: boolean
  preenchida: boolean
}

/** Despacha para o formulário 1:1, a lista ou o recordatório aninhado. */
function PainelSecao({ secao, atendimento, editavel, preenchida }: PainelSecaoProps) {
  const meta = META_SECAO[secao]
  const comum = {
    atendimentoId: atendimento.id,
    titulo: meta.titulo,
    descricao: meta.descricao,
    preenchida,
    editavel,
  }

  switch (secao) {
    case 'QUEIXA_PRINCIPAL':
      return (
        <SecaoFormulario<FormQueixa>
          {...comum}
          secao={secao}
          campos={CAMPOS_QUEIXA}
          padroes={PADRAO_QUEIXA}
          valores={atendimento.queixaPrincipal as Partial<FormQueixa> | undefined}
        />
      )
    case 'HISTORIA_CLINICA':
      return (
        <SecaoFormulario<FormHistoria>
          {...comum}
          secao={secao}
          campos={CAMPOS_HISTORIA}
          padroes={PADRAO_HISTORIA}
          valores={atendimento.historiaClinica as Partial<FormHistoria> | undefined}
        />
      )
    case 'ANTROPOMETRIA':
      return (
        <SecaoFormulario<FormAntropometria, Antropometria>
          {...comum}
          secao={secao}
          campos={CAMPOS_ANTROPOMETRIA}
          padroes={PADRAO_ANTROPOMETRIA}
          valores={atendimento.antropometria as Partial<FormAntropometria> | undefined}
          // Os indicadores da resposta do PATCH ganham do que veio no GET.
          extra={(_form, resposta) => (
            <PainelCalculados dados={resposta ?? atendimento.antropometria} />
          )}
        />
      )
    case 'FREQUENCIA_ALIMENTAR':
      return (
        <SecaoFormulario<FormFrequencia>
          {...comum}
          secao={secao}
          campos={CAMPOS_FREQUENCIA}
          padroes={PADRAO_FREQUENCIA}
          valores={atendimento.frequenciaAlimentar as Partial<FormFrequencia> | undefined}
        />
      )
    case 'COMPORTAMENTO_ALIMENTAR':
      return (
        <SecaoFormulario<FormComportamento>
          {...comum}
          secao={secao}
          campos={CAMPOS_COMPORTAMENTO}
          padroes={PADRAO_COMPORTAMENTO}
          valores={atendimento.comportamentoAlimentar as Partial<FormComportamento> | undefined}
        />
      )
    case 'DIAGNOSTICO':
      return (
        <SecaoFormulario<FormDiagnostico>
          {...comum}
          secao={secao}
          campos={CAMPOS_DIAGNOSTICO}
          padroes={PADRAO_DIAGNOSTICO}
          valores={atendimento.diagnostico as Partial<FormDiagnostico> | undefined}
          extra={(form) => <SentencaPes form={form} />}
        />
      )
    case 'PLANO':
      return (
        <SecaoFormulario<FormPlano>
          {...comum}
          secao={secao}
          campos={CAMPOS_PLANO}
          padroes={PADRAO_PLANO}
          valores={atendimento.plano as Partial<FormPlano> | undefined}
        />
      )
    case 'MEDICAMENTOS':
      return (
        <SecaoLista<FormMedicamento>
          {...comum}
          secao={secao}
          rotuloItem={ROTULO_ITEM.MEDICAMENTOS?.rotulo ?? 'Item'}
          textoVazio={ROTULO_ITEM.MEDICAMENTOS?.vazio ?? ''}
          campos={CAMPOS_MEDICAMENTO}
          padraoItem={PADRAO_MEDICAMENTO}
          valores={atendimento.medicamentos as FormMedicamento[] | undefined}
        />
      )
    case 'EXAMES':
      return (
        <SecaoLista<FormExame>
          {...comum}
          secao={secao}
          rotuloItem={ROTULO_ITEM.EXAMES?.rotulo ?? 'Item'}
          textoVazio={ROTULO_ITEM.EXAMES?.vazio ?? ''}
          campos={CAMPOS_EXAME}
          padraoItem={PADRAO_EXAME}
          valores={atendimento.exames as FormExame[] | undefined}
        />
      )
    case 'METAS':
      return (
        <SecaoLista<FormMeta>
          {...comum}
          secao={secao}
          rotuloItem={ROTULO_ITEM.METAS?.rotulo ?? 'Item'}
          textoVazio={ROTULO_ITEM.METAS?.vazio ?? ''}
          campos={CAMPOS_META}
          padraoItem={PADRAO_META}
          valores={atendimento.metas as FormMeta[] | undefined}
        />
      )
    case 'RECORDATORIO':
      return (
        <SecaoRecordatorio
          atendimentoId={atendimento.id}
          titulo={meta.titulo}
          descricao={meta.descricao}
          preenchida={preenchida}
          valores={atendimento.recordatorio}
          editavel={editavel}
        />
      )
  }
}

/** Prévia da sentença PES montada a partir dos três campos do diagnóstico. */
function SentencaPes({ form }: { form: { watch: (nome: string) => string } }) {
  const problema = form.watch('problema') || '[problema]'
  const etiologia = form.watch('etiologia') || '[etiologia]'
  const sinais = form.watch('sinaisSintomas') || '[sinais e sintomas]'

  return (
    <div className="bg-surface-alt border-line-subtle mt-4 rounded-2xl border px-4 py-3.5">
      <p className="text-ink-muted mb-1.5 text-xs font-semibold tracking-wide uppercase">
        Sentença PES
      </p>
      <p className="text-ink-body text-lg leading-loose">
        {problema} relacionado a {etiologia}, evidenciado por {sinais}.
      </p>
    </div>
  )
}
