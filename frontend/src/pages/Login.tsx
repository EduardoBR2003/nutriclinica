import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Navigate, useLocation, useNavigate } from 'react-router'
import { z } from 'zod'

import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useAuth } from '@/hooks/useAuth'
import { useLogin } from '@/hooks/useLogin'
import { aplicarErrosDeCampo, mensagemDeErro } from '@/lib/erros'

const esquema = z.object({
  email: z.email('Informe um e-mail válido.'),
  senha: z.string().min(1, 'Informe a senha.'),
})

type FormularioLogin = z.infer<typeof esquema>

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
      <main className="flex min-h-svh items-center justify-center">
        <p className="text-muted-foreground text-sm">Carregando…</p>
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

  return (
    <main className="flex min-h-svh items-center justify-center p-6">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>Entrar</CardTitle>
          <CardDescription>Prontuário da clínica escola de nutrição</CardDescription>
        </CardHeader>

        <CardContent>
          <form onSubmit={aoEnviar} noValidate className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="email">E-mail</Label>
              <Input
                id="email"
                type="email"
                autoComplete="username"
                autoFocus
                aria-invalid={Boolean(errors.email)}
                {...register('email')}
              />
              {errors.email && (
                <p className="text-destructive text-sm">{errors.email.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="senha">Senha</Label>
              <Input
                id="senha"
                type="password"
                autoComplete="current-password"
                aria-invalid={Boolean(errors.senha)}
                {...register('senha')}
              />
              {errors.senha && (
                <p className="text-destructive text-sm">{errors.senha.message}</p>
              )}
            </div>

            {login.isError && (
              <p role="alert" className="text-destructive text-sm">
                {mensagemDeErro(login.error)}
              </p>
            )}

            <Button type="submit" className="w-full" disabled={login.isPending}>
              {login.isPending ? 'Entrando…' : 'Entrar'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </main>
  )
}
