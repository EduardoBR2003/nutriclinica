import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link, Navigate } from 'react-router'
import { z } from 'zod'

import { Botao } from '@/components/Botao'
import { Logo } from '@/components/Logo'
import { useAuth } from '@/hooks/useAuth'
import { useCadastro } from '@/hooks/useUsuarios'
import { aplicarErrosDeCampo, mensagemDeErro } from '@/lib/erros'
import { cn } from '@/lib/utils'

const esquema = z
  .object({
    nome: z.string().min(3, 'Mínimo de 3 caracteres.').max(150, 'Máximo de 150 caracteres.'),
    email: z.email('Informe um e-mail válido.'),
    senha: z.string().min(8, 'Mínimo de 8 caracteres.').max(72, 'Máximo de 72 caracteres.'),
    confirmacao: z.string(),
    perfil: z.enum(['ESTAGIARIO', 'SUPERVISOR']),
  })
  .refine((valores) => valores.senha === valores.confirmacao, {
    path: ['confirmacao'],
    message: 'As senhas não conferem.',
  })

type FormularioCadastro = z.infer<typeof esquema>

const CAMPO =
  'text-ink bg-surface border-line focus:border-brand-muted focus:ring-brand-tint-hover h-11 w-full rounded-xl border px-3.5 text-xl outline-none focus:ring-3'

export default function Cadastro() {
  const { usuario, carregando } = useAuth()
  const cadastro = useCadastro()

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<FormularioCadastro>({
    resolver: zodResolver(esquema),
    defaultValues: { nome: '', email: '', senha: '', confirmacao: '', perfil: 'ESTAGIARIO' },
  })

  if (carregando) {
    return (
      <main className="grid min-h-svh place-items-center">
        <p className="text-ink-muted text-lg">Carregando…</p>
      </main>
    )
  }

  // Quem já tem sessão não se cadastra de novo.
  if (usuario) return <Navigate to="/" replace />

  const aoEnviar = handleSubmit(async (valores) => {
    try {
      await cadastro.mutateAsync({
        nome: valores.nome,
        email: valores.email,
        senha: valores.senha,
        perfil: valores.perfil,
      })
    } catch (erro) {
      // O 422 do contrato traz `campos` — e-mail já cadastrado cai em `email`.
      aplicarErrosDeCampo<FormularioCadastro>(erro, setError, ['nome', 'email', 'senha', 'perfil'])
    }
  })

  const criado = cadastro.data

  return (
    <main className="grid min-h-svh place-items-center px-5 py-10">
      <div className="w-[min(460px,100%)]">
        <div className="mb-[22px] flex items-center gap-3">
          <Logo className="h-[58px]" />
          <span className="text-3xl font-bold tracking-snug">NutriClinica</span>
        </div>

        <div className="bg-surface border-line-card shadow-modal rounded-4xl border p-[26px]">
          {criado ? (
            <>
              <h1 className="text-5xl font-bold tracking-tight">Cadastro enviado</h1>
              <p className="text-ink-muted mt-1.5 mb-5 text-md leading-relaxed">
                A conta de <strong className="text-ink font-semibold">{criado.nome}</strong> foi
                criada e está aguardando liberação. Um administrador precisa ativá-la antes do
                primeiro acesso — por se tratar de um sistema com dados de saúde, ninguém entra
                sem essa conferência.
              </p>
              <Link
                to="/login"
                className="bg-brand hover:bg-brand-hover flex h-11 w-full items-center justify-center rounded-xl text-xl font-semibold text-white"
              >
                Voltar para o login
              </Link>
            </>
          ) : (
            <>
              <h1 className="text-5xl font-bold tracking-tight">Criar conta</h1>
              <p className="text-ink-muted mt-1.5 mb-5 text-md leading-relaxed">
                Use o e-mail institucional. A conta fica pendente até um administrador liberar o
                acesso.
              </p>

              {cadastro.isError ? (
                <div
                  role="alert"
                  className="bg-danger-tint border-danger-border mb-4 flex gap-2.5 rounded-xl border px-3.5 py-3"
                >
                  <span className="bg-cta mt-px inline-flex size-[18px] shrink-0 items-center justify-center rounded-full text-base font-bold text-white">
                    !
                  </span>
                  <p className="text-danger-text min-w-0 text-base leading-normal">
                    {mensagemDeErro(cadastro.error)}
                  </p>
                </div>
              ) : null}

              <form onSubmit={aoEnviar} noValidate className="flex flex-col gap-3.5">
                <div>
                  <label
                    htmlFor="nome"
                    className="text-ink-label mb-1.5 block text-base font-semibold"
                  >
                    Nome completo
                  </label>
                  <input
                    id="nome"
                    type="text"
                    autoComplete="name"
                    autoFocus
                    placeholder="Como aparece na lista de chamada"
                    aria-invalid={Boolean(errors.nome)}
                    {...register('nome')}
                    className={cn(CAMPO, errors.nome && 'border-cta')}
                  />
                  {errors.nome ? (
                    <p className="text-cta-text mt-1.5 text-sm font-semibold">
                      {errors.nome.message}
                    </p>
                  ) : null}
                </div>

                <div>
                  <label
                    htmlFor="email"
                    className="text-ink-label mb-1.5 block text-base font-semibold"
                  >
                    E-mail
                  </label>
                  <input
                    id="email"
                    type="email"
                    autoComplete="username"
                    placeholder="nome@instituicao.edu.br"
                    aria-invalid={Boolean(errors.email)}
                    {...register('email')}
                    className={cn(CAMPO, errors.email && 'border-cta')}
                  />
                  {errors.email ? (
                    <p className="text-cta-text mt-1.5 text-sm font-semibold">
                      {errors.email.message}
                    </p>
                  ) : null}
                </div>

                <div>
                  <label
                    htmlFor="perfil"
                    className="text-ink-label mb-1.5 block text-base font-semibold"
                  >
                    Você é
                  </label>
                  <select
                    id="perfil"
                    {...register('perfil')}
                    className={cn(CAMPO, 'cursor-pointer appearance-none')}
                  >
                    <option value="ESTAGIARIO">Estagiário de Nutrição</option>
                    <option value="SUPERVISOR">Supervisor (professor)</option>
                  </select>
                  <p className="text-ink-muted mt-1.5 text-xs">
                    Conta de administrador é criada apenas por outro administrador.
                  </p>
                </div>

                <div>
                  <label
                    htmlFor="senha"
                    className="text-ink-label mb-1.5 block text-base font-semibold"
                  >
                    Senha
                  </label>
                  <input
                    id="senha"
                    type="password"
                    autoComplete="new-password"
                    placeholder="mínimo 8 caracteres"
                    aria-invalid={Boolean(errors.senha)}
                    {...register('senha')}
                    className={cn(CAMPO, errors.senha && 'border-cta')}
                  />
                  {errors.senha ? (
                    <p className="text-cta-text mt-1.5 text-sm font-semibold">
                      {errors.senha.message}
                    </p>
                  ) : null}
                </div>

                <div>
                  <label
                    htmlFor="confirmacao"
                    className="text-ink-label mb-1.5 block text-base font-semibold"
                  >
                    Repita a senha
                  </label>
                  <input
                    id="confirmacao"
                    type="password"
                    autoComplete="new-password"
                    placeholder="••••••••"
                    aria-invalid={Boolean(errors.confirmacao)}
                    {...register('confirmacao')}
                    className={cn(CAMPO, errors.confirmacao && 'border-cta')}
                  />
                  {errors.confirmacao ? (
                    <p className="text-cta-text mt-1.5 text-sm font-semibold">
                      {errors.confirmacao.message}
                    </p>
                  ) : null}
                </div>

                <Botao type="submit" tamanho="bloco" className="mt-1" disabled={cadastro.isPending}>
                  {cadastro.isPending ? 'Enviando…' : 'Criar conta'}
                </Botao>
              </form>

              <p className="text-ink-muted mt-4 text-center text-base">
                Já tem acesso?{' '}
                <Link to="/login" className="text-brand-text font-semibold hover:underline">
                  Entrar
                </Link>
              </p>
            </>
          )}
        </div>
      </div>
    </main>
  )
}
