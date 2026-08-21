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
import { useToast } from '@/hooks/useToast'
import { useSalvarUsuario, useUsuario } from '@/hooks/useUsuarios'
import { aplicarErrosDeCampo } from '@/lib/erros'
import { PERFIL, opcoesDe } from '@/lib/rotulos'
import type { UsuarioRequest } from '@/types/dominio'

const esquema = z.object({
  nome: z.string().min(3, 'Mínimo de 3 caracteres.').max(150, 'Máximo de 150 caracteres.'),
  email: z.email('E-mail inválido.'),
  senha: z.string(),
  perfil: z.enum(['ESTAGIARIO', 'SUPERVISOR', 'ADMIN']),
  ativo: z.boolean(),
})

type FormularioUsuario = z.infer<typeof esquema>

export default function UsuarioForm() {
  const { id } = useParams()
  const usuarioId = id ? Number(id) : undefined
  const navigate = useNavigate()
  const { avisar } = useToast()

  const consulta = useUsuario(usuarioId)
  const salvar = useSalvarUsuario()

  const form = useForm<FormularioUsuario>({
    resolver: zodResolver(esquema),
    defaultValues: { nome: '', email: '', senha: '', perfil: 'ESTAGIARIO', ativo: true },
  })
  const { reset } = form

  useEffect(() => {
    const usuario = consulta.data
    if (!usuario) return
    reset({
      nome: usuario.nome,
      email: usuario.email,
      senha: '',
      perfil: usuario.perfil,
      ativo: usuario.ativo,
    })
  }, [consulta.data, reset])

  const campos: readonly CampoDef<FormularioUsuario>[] = [
    { nome: 'nome', label: 'Nome', tipo: 'texto', obrigatorio: true, largo: true },
    { nome: 'email', label: 'E-mail', tipo: 'texto', obrigatorio: true, largo: true },
    {
      nome: 'senha',
      label: 'Senha',
      tipo: 'senha',
      obrigatorio: usuarioId === undefined,
      ajuda: usuarioId === undefined ? 'mínimo 8 caracteres' : 'deixe em branco para manter',
    },
    { nome: 'perfil', label: 'Perfil', tipo: 'select', obrigatorio: true, opcoes: opcoesDe(PERFIL) },
    { nome: 'ativo', label: 'Usuário ativo', tipo: 'bool' },
  ]

  const aoEnviar = form.handleSubmit(async (valores) => {
    if (usuarioId === undefined && valores.senha.length < 8) {
      form.setError('senha', { type: 'manual', message: 'Mínimo de 8 caracteres.' })
      return
    }
    const corpo: UsuarioRequest = {
      nome: valores.nome,
      email: valores.email,
      perfil: valores.perfil,
      ativo: valores.ativo,
      // Senha em branco na edição significa "manter a atual".
      senha: valores.senha || undefined,
    }
    try {
      const salvo = await salvar.mutateAsync({ id: usuarioId, corpo })
      avisar(usuarioId === undefined ? 'Usuário criado' : 'Usuário atualizado', salvo.nome)
      navigate('/admin/usuarios')
    } catch (erro) {
      aplicarErrosDeCampo<FormularioUsuario>(erro, form.setError, [
        'nome',
        'email',
        'senha',
        'perfil',
        'ativo',
      ])
    }
  })

  return (
    <>
      <CabecalhoPagina
        titulo={usuarioId === undefined ? 'Novo usuário' : 'Editar usuário'}
        subtitulo="Perfil define as permissões; usuário inativo não consegue entrar."
      />

      {salvar.isError ? <BannerErro erro={salvar.error} /> : null}
      {consulta.isError ? <BannerErro erro={consulta.error} /> : null}

      <form onSubmit={aoEnviar} noValidate className="flex max-w-[760px] flex-col gap-4">
        <Painel>
          <h2 className="mb-4 text-3xl font-bold tracking-snug">Dados do usuário</h2>
          <GradeCampos form={form} campos={campos} />
        </Painel>
        <div className="flex justify-end gap-2.5">
          <Botao
            type="button"
            variante="contorno"
            tamanho="lg"
            onClick={() => navigate('/admin/usuarios')}
          >
            Cancelar
          </Botao>
          <Botao type="submit" tamanho="lg" disabled={salvar.isPending}>
            {salvar.isPending ? 'Salvando…' : 'Salvar usuário'}
          </Botao>
        </div>
      </form>
    </>
  )
}
