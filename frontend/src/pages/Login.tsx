import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'
import { z } from 'zod'

import { Botao } from '@/components/Botao'
import { Logo } from '@/components/Logo'
import { useAuth } from '@/hooks/useAuth'
import { useLogin } from '@/hooks/useLogin'
import { aplicarErrosDeCampo, extrairErro } from '@/lib/erros'
import { cn } from '@/lib/utils'

const esquema = z.object({
  email: z.email('Informe um e-mail válido.'),
  senha: z.string().min(1, 'Informe a senha.'),
})

type FormularioLogin = z.infer<typeof esquema>

const CAMPO =
  'text-ink bg-surface border-line focus:border-brand-muted focus:ring-brand-tint-hover h-11 w-full rounded-xl border px-3.5 text-xl outline-none focus:ring-3'

export default function Login() {
  const { usuario, carregando } = useAuth()
  const login = useLogin()
  const navigate = useNavigate()
  const location = useLocation()

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<FormularioLogin>({
    resolver: zodResolver(esquema),
    defaultValues: { email: '', senha: '' },
  })

  // Destino guardado pela RotaProtegida quando ela interceptou a navegação.
  const destino = (location.state as { de?: string } | null)?.de ?? '/'

  if (carregando) {
    return (
      <main className="grid min-h-svh place-items-center">
        <p className="text-ink-muted text-lg">Carregando…</p>
      </main>
    )
  }

  if (usuario) return <Navigate to={destino} replace />

  const aoEnviar = handleSubmit(async (valores) => {
    try {
      await login.mutateAsync(valores)
      navigate(destino, { replace: true })
    } catch (erro) {
      // 422 do contrato traz `campos`; 401 não traz, e vira mensagem geral.
      aplicarErrosDeCampo<FormularioLogin>(erro, setError, ['email', 'senha'])
    }
  })

  const falha = login.isError ? extrairErro(login.error) : null

  return (
    <main className="grid min-h-svh place-items-center px-5 py-10">
      <div className="w-[min(420px,100%)]">
        <div className="mb-[22px] flex items-center gap-3">
          <Logo className="h-[58px]" />
          <span className="text-3xl font-bold tracking-snug">NutriClinica</span>
        </div>

        <div className="bg-surface border-line-card shadow-modal rounded-4xl border p-[26px]">
          <h1 className="text-5xl font-bold tracking-tight">Entrar na plataforma</h1>
          <p className="text-ink-muted mt-1.5 mb-5 text-md leading-relaxed">
            Use o e-mail institucional cadastrado pela coordenação.
          </p>

          {falha ? (
            <div
              role="alert"
              className="bg-danger-tint border-danger-border mb-4 flex gap-2.5 rounded-xl border px-3.5 py-3"
            >
              <span className="bg-cta mt-px inline-flex size-[18px] shrink-0 items-center justify-center rounded-full text-base font-bold text-white">
                !
              </span>
              <div className="min-w-0">
                <p className="text-cta-text-deep text-base font-bold">
                  {falha.codigo ?? 'NAO_AUTORIZADO'}
                  {falha.status ? ` · HTTP ${falha.status}` : ''}
                </p>
                <p className="text-danger-text mt-0.5 text-base leading-normal">
                  {falha.mensagem}
                </p>
              </div>
            </div>
          ) : null}

          <form onSubmit={aoEnviar} noValidate className="flex flex-col gap-3.5">
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
                autoFocus
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
                htmlFor="senha"
                className="text-ink-label mb-1.5 block text-base font-semibold"
              >
                Senha
              </label>
              <input
                id="senha"
                type="password"
                autoComplete="current-password"
                placeholder="••••••••"
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

            <Botao type="submit" tamanho="bloco" className="mt-1" disabled={login.isPending}>
              {login.isPending ? 'Entrando…' : 'Entrar'}
            </Botao>
          </form>

          <p className="text-ink-muted mt-4 text-center text-base">
            Ainda não tem acesso?{' '}
            <Link to="/cadastro" className="text-brand-text font-semibold hover:underline">
              Criar conta
            </Link>
          </p>
        </div>
      </div>
    </main>
  )
}
