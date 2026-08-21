import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate, useParams } from 'react-router'
import { z } from 'zod'

import { BannerErro } from '@/components/BannerErro'
import { Botao } from '@/components/Botao'
import { CabecalhoPagina } from '@/components/CabecalhoPagina'
import { GradeCampos } from '@/components/campos/GradeCampos'
import type { CampoDef } from '@/components/campos/tipos'
import { Painel } from '@/components/Painel'
import { usePaciente, useSalvarPaciente } from '@/hooks/usePacientes'
import { useToast } from '@/hooks/useToast'
import { aplicarErrosDeCampo } from '@/lib/erros'
import { RACA_COR, SEXO, opcoesDe } from '@/lib/rotulos'
import type { PacienteRequest } from '@/types/dominio'

const esquema = z.object({
  nome: z.string().min(3, 'Mínimo de 3 caracteres.').max(150, 'Máximo de 150 caracteres.'),
  dataNascimento: z.string().min(1, 'Informe a data de nascimento.'),
  sexo: z.enum(['FEMININO', 'MASCULINO', 'OUTRO', 'NAO_INFORMADO']),
  racaCor: z.enum(['BRANCA', 'PRETA', 'PARDA', 'AMARELA', 'INDIGENA', 'NAO_INFORMADO']),
  telefone: z.string(),
  email: z.union([z.literal(''), z.email('E-mail inválido.')]),
})

type FormularioPaciente = z.infer<typeof esquema>

const CAMPOS: readonly CampoDef<FormularioPaciente>[] = [
  {
    nome: 'nome',
    label: 'Nome completo',
    tipo: 'texto',
    obrigatorio: true,
    largo: true,
    ajuda: '3 a 150 caracteres',
  },
  { nome: 'dataNascimento', label: 'Data de nascimento', tipo: 'data', obrigatorio: true },
  { nome: 'sexo', label: 'Sexo', tipo: 'select', obrigatorio: true, opcoes: opcoesDe(SEXO) },
  { nome: 'racaCor', label: 'Raça/cor', tipo: 'select', opcoes: opcoesDe(RACA_COR) },
  { nome: 'telefone', label: 'Telefone', tipo: 'texto', dica: '(11) 90000-0000' },
  { nome: 'email', label: 'E-mail', tipo: 'texto', dica: 'opcional' },
]

const PADROES: FormularioPaciente = {
  nome: '',
  dataNascimento: '',
  sexo: 'FEMININO',
  racaCor: 'NAO_INFORMADO',
  telefone: '',
  email: '',
}

export default function PacienteForm() {
  const { id } = useParams()
  const pacienteId = id ? Number(id) : undefined
  const navigate = useNavigate()
  const { avisar } = useToast()

  const consulta = usePaciente(pacienteId)
  const salvar = useSalvarPaciente()

  const form = useForm<FormularioPaciente>({
    resolver: zodResolver(esquema),
    defaultValues: PADROES,
  })
  const { reset } = form

  useEffect(() => {
    const paciente = consulta.data
    if (!paciente) return
    reset({
      nome: paciente.nome,
      dataNascimento: paciente.dataNascimento,
      sexo: paciente.sexo,
      racaCor: paciente.racaCor ?? 'NAO_INFORMADO',
      telefone: paciente.telefone ?? '',
      email: paciente.email ?? '',
    })
  }, [consulta.data, reset])

  const aoEnviar = form.handleSubmit(async (valores) => {
    const corpo: PacienteRequest = {
      ...valores,
      telefone: valores.telefone || undefined,
      email: valores.email || undefined,
    }
    try {
      const salvo = await salvar.mutateAsync({ id: pacienteId, corpo })
      if (pacienteId === undefined) {
        avisar('Paciente cadastrado', 'Registre o termo de consentimento para liberar atendimentos.')
        // Sem termo LGPD o atendimento nem pode ser aberto — o design leva direto para lá.
        navigate(`/pacientes/${salvo.id}/termo`)
      } else {
        avisar('Paciente atualizado', salvo.nome)
        navigate('/pacientes')
      }
    } catch (erro) {
      aplicarErrosDeCampo<FormularioPaciente>(erro, form.setError, [
        'nome',
        'dataNascimento',
        'sexo',
        'racaCor',
        'telefone',
        'email',
      ])
    }
  })

  return (
    <>
      <CabecalhoPagina
        titulo={pacienteId === undefined ? 'Novo paciente' : 'Editar paciente'}
        subtitulo="A idade é derivada da data de nascimento pelo servidor."
      />

      {salvar.isError ? <BannerErro erro={salvar.error} /> : null}
      {consulta.isError ? <BannerErro erro={consulta.error} /> : null}

      <form onSubmit={aoEnviar} noValidate className="flex max-w-[820px] flex-col gap-4">
        <Painel>
          <h2 className="mb-4 text-3xl font-bold tracking-snug">Identificação</h2>
          <GradeCampos form={form} campos={CAMPOS} />
        </Painel>
        <div className="flex justify-end gap-2.5">
          <Botao
            type="button"
            variante="contorno"
            tamanho="lg"
            onClick={() => navigate('/pacientes')}
          >
            Cancelar
          </Botao>
          <Botao type="submit" tamanho="lg" disabled={salvar.isPending}>
            {salvar.isPending ? 'Salvando…' : 'Salvar paciente'}
          </Botao>
        </div>
      </form>
    </>
  )
}
