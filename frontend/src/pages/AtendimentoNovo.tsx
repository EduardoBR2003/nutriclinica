import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router'

import { BannerErro } from '@/components/BannerErro'
import { Botao } from '@/components/Botao'
import { CabecalhoPagina } from '@/components/CabecalhoPagina'
import { GradeCampos } from '@/components/campos/GradeCampos'
import type { CampoDef, OpcaoCampo } from '@/components/campos/tipos'
import { Painel } from '@/components/Painel'
import { useCriarAtendimento } from '@/hooks/useAtendimentos'
import { usePacientes } from '@/hooks/usePacientes'
import { useToast } from '@/hooks/useToast'
import { useSupervisoresDisponiveis } from '@/hooks/useUsuarios'
import { aplicarErrosDeCampo } from '@/lib/erros'
import { hojeIso } from '@/lib/formato'

interface FormularioAtendimento {
  pacienteId: string
  supervisorId: string
  dataConsulta: string
}

export default function AtendimentoNovo() {
  const navigate = useNavigate()
  const { avisar } = useToast()

  const pacientes = usePacientes({ page: 0, size: 100 })
  const supervisores = useSupervisoresDisponiveis()
  const criar = useCriarAtendimento()

  const form = useForm<FormularioAtendimento>({
    defaultValues: { pacienteId: '', supervisorId: '', dataConsulta: hojeIso() },
  })

  const opcoesPaciente: OpcaoCampo[] = [
    { valor: '', texto: 'Selecione…' },
    ...(pacientes.data?.content ?? []).map((paciente) => ({
      valor: String(paciente.id),
      // O 409 SEM_TERMO_CONSENTIMENTO é do servidor; aqui só se avisa antes.
      texto: paciente.possuiTermo ? paciente.nome : `${paciente.nome} — sem termo`,
    })),
  ]

  // Só os supervisores que orientam o estagiário: são os únicos que o
  // POST /api/atendimentos aceita, e oferecer outro só produziria um 422.
  const listaSupervisores = supervisores.data ?? []
  const semSupervisor = supervisores.isSuccess && listaSupervisores.length === 0

  const opcoesSupervisor: OpcaoCampo[] = [
    { valor: '', texto: semSupervisor ? 'Nenhum supervisor vinculado' : 'Selecione…' },
    ...listaSupervisores.map((usuario) => ({
      valor: String(usuario.id),
      texto: usuario.nome,
    })),
  ]

  const campos: readonly CampoDef<FormularioAtendimento>[] = [
    {
      nome: 'pacienteId',
      label: 'Paciente',
      tipo: 'select',
      obrigatorio: true,
      opcoes: opcoesPaciente,
    },
    {
      nome: 'supervisorId',
      label: 'Supervisor',
      tipo: 'select',
      obrigatorio: true,
      opcoes: opcoesSupervisor,
      ajuda: supervisores.isError ? undefined : 'Só supervisores que orientam você.',
    },
    { nome: 'dataConsulta', label: 'Data da consulta', tipo: 'data', obrigatorio: true },
  ]

  const aoEnviar = form.handleSubmit(async (valores) => {
    let invalido = false
    if (!valores.pacienteId) {
      form.setError('pacienteId', { type: 'manual', message: 'Selecione o paciente.' })
      invalido = true
    }
    if (!valores.supervisorId) {
      form.setError('supervisorId', { type: 'manual', message: 'Selecione o supervisor.' })
      invalido = true
    }
    if (!valores.dataConsulta) {
      form.setError('dataConsulta', { type: 'manual', message: 'Informe a data da consulta.' })
      invalido = true
    }
    if (invalido) return

    try {
      const atendimento = await criar.mutateAsync({
        pacienteId: Number(valores.pacienteId),
        supervisorId: Number(valores.supervisorId),
        dataConsulta: valores.dataConsulta,
      })
      avisar('Atendimento aberto', `Prontuário ${atendimento.numeroProntuario} criado como rascunho.`)
      navigate(`/atendimentos/${atendimento.id}`)
    } catch (erro) {
      aplicarErrosDeCampo<FormularioAtendimento>(erro, form.setError, [
        'pacienteId',
        'supervisorId',
        'dataConsulta',
      ])
    }
  })

  return (
    <>
      <CabecalhoPagina
        titulo="Abrir atendimento"
        subtitulo="Escolha paciente, supervisor e data da consulta."
      />

      {criar.isError ? <BannerErro erro={criar.error} /> : null}

      {supervisores.isError ? <BannerErro erro={supervisores.error} /> : null}

      {semSupervisor ? (
        <div
          role="alert"
          className="bg-cta-tint border-cta-border text-cta-text rounded-3xl border p-4 text-lg leading-relaxed"
        >
          <strong className="font-semibold">Nenhum supervisor vinculado a você.</strong> Quem
          revisa o atendimento é o supervisor designado, e o vínculo é criado pelo administrador
          em <em>Vínculos</em>. Peça a ele para vincular você a um supervisor antes de abrir o
          prontuário.
        </div>
      ) : null}

      <form onSubmit={aoEnviar} noValidate className="flex max-w-[760px] flex-col gap-4">
        <Painel>
          <h2 className="mb-1 text-3xl font-bold tracking-snug">Abrir atendimento</h2>
          <p className="text-ink-muted mb-4 text-base leading-normal">
            O número do prontuário é gerado pelo servidor e o estagiário é o usuário autenticado.
          </p>
          <GradeCampos form={form} campos={campos} />
        </Painel>
        <div className="flex justify-end gap-2.5">
          <Botao
            type="button"
            variante="contorno"
            tamanho="lg"
            onClick={() => navigate('/atendimentos')}
          >
            Cancelar
          </Botao>
          <Botao type="submit" tamanho="lg" disabled={criar.isPending || semSupervisor}>
            {criar.isPending ? 'Abrindo…' : 'Abrir prontuário'}
          </Botao>
        </div>
      </form>
    </>
  )
}
