import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate, useParams } from 'react-router'

import { BannerErro } from '@/components/BannerErro'
import { Botao } from '@/components/Botao'
import { CabecalhoPagina } from '@/components/CabecalhoPagina'
import { Chip } from '@/components/Chip'
import { GradeCampos } from '@/components/campos/GradeCampos'
import type { CampoDef } from '@/components/campos/tipos'
import { Painel } from '@/components/Painel'
import { usePaciente, useSalvarTermo, useTermo } from '@/hooks/usePacientes'
import { useToast } from '@/hooks/useToast'
import { extrairErro } from '@/lib/erros'
import { formatarData, hojeIso } from '@/lib/formato'

interface FormularioTermo {
  aceiteLgpd: boolean
  dataAceite: string
  autorizaUsoPesquisa: boolean
  observacoes: string
}

/**
 * `dataAceite` aparece travada: é o dia do aceite, carimbado pelo servidor no
 * momento do registro e imutável depois disso. Deixá-la digitável convidaria a
 * antedatar um consentimento — o oposto do que a LGPD pede de um termo.
 */
const CAMPOS: readonly CampoDef<FormularioTermo>[] = [
  {
    nome: 'aceiteLgpd',
    label: 'Termo LGPD assinado pelo paciente',
    tipo: 'bool',
    obrigatorio: true,
    largo: true,
  },
  {
    nome: 'dataAceite',
    label: 'Data do aceite',
    tipo: 'data',
    somenteLeitura: true,
    ajuda: 'Registrada pelo servidor no dia do aceite.',
  },
  { nome: 'autorizaUsoPesquisa', label: 'Autoriza uso em pesquisa e ensino', tipo: 'bool' },
  { nome: 'observacoes', label: 'Observações', tipo: 'area', largo: true },
]

export default function Termo() {
  const { id } = useParams()
  const pacienteId = Number(id)
  const navigate = useNavigate()
  const { avisar } = useToast()

  const paciente = usePaciente(pacienteId)
  const termo = useTermo(pacienteId)
  const salvar = useSalvarTermo(pacienteId)

  const form = useForm<FormularioTermo>({
    defaultValues: {
      aceiteLgpd: false,
      // Termo novo: a data que o servidor vai carimbar é a de hoje.
      dataAceite: hojeIso(),
      autorizaUsoPesquisa: false,
      observacoes: '',
    },
  })
  const { reset } = form

  useEffect(() => {
    const dados = termo.data
    if (!dados) return
    reset({
      aceiteLgpd: dados.aceiteLgpd ?? false,
      dataAceite: dados.dataAceite ?? hojeIso(),
      autorizaUsoPesquisa: dados.autorizaUsoPesquisa ?? false,
      observacoes: dados.observacoes ?? '',
    })
  }, [termo.data, reset])

  const aoEnviar = form.handleSubmit(async (valores) => {
    if (!valores.aceiteLgpd) {
      form.setError('aceiteLgpd', { type: 'manual', message: 'O aceite é obrigatório.' })
      return
    }
    // `dataAceite` não vai no corpo: quem a define é o servidor.
    await salvar.mutateAsync({
      aceiteLgpd: valores.aceiteLgpd,
      autorizaUsoPesquisa: valores.autorizaUsoPesquisa,
      observacoes: valores.observacoes || undefined,
    })
    avisar('Termo registrado', `${paciente.data?.nome ?? 'O paciente'} já pode ter atendimentos abertos.`)
    navigate('/pacientes')
  })

  // 404 aqui significa "ainda não tem termo", que é o caminho normal de cadastro.
  const semTermo = termo.isError && extrairErro(termo.error).status === 404
  const registrado = Boolean(termo.data?.aceiteLgpd)

  return (
    <>
      <CabecalhoPagina
        titulo="Termo de consentimento"
        subtitulo="Sem termo registrado não é possível abrir atendimento."
      />

      {salvar.isError ? <BannerErro erro={salvar.error} /> : null}
      {termo.isError && !semTermo ? <BannerErro erro={termo.error} /> : null}

      <form onSubmit={aoEnviar} noValidate className="flex max-w-[760px] flex-col gap-4">
        <Painel>
          <div className="mb-4 flex flex-wrap items-start justify-between gap-3.5">
            <div>
              <h2 className="text-3xl font-bold tracking-snug">Termo de consentimento</h2>
              <p className="text-ink-muted mt-1 text-base">
                {paciente.data
                  ? `${paciente.data.nome} · ${paciente.data.idade} anos`
                  : 'Carregando paciente…'}
              </p>
            </div>
            <Chip tom={registrado ? 'sucesso' : 'alerta'}>
              {registrado ? 'termo registrado' : 'sem termo'}
            </Chip>
          </div>

          <GradeCampos form={form} campos={CAMPOS} />

          {termo.data?.registradoPor ? (
            <p className="text-ink-muted mt-4 text-base">
              Registrado por {termo.data.registradoPor}
              {termo.data.dataAceite ? ` · aceite em ${formatarData(termo.data.dataAceite)}` : ''}
            </p>
          ) : null}
        </Painel>

        <div className="flex justify-end gap-2.5">
          <Botao
            type="button"
            variante="contorno"
            tamanho="lg"
            onClick={() => navigate('/pacientes')}
          >
            Voltar
          </Botao>
          <Botao type="submit" tamanho="lg" disabled={salvar.isPending}>
            {salvar.isPending ? 'Registrando…' : 'Registrar termo'}
          </Botao>
        </div>
      </form>
    </>
  )
}
