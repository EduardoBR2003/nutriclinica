import { Link } from 'react-router'

import { Button } from '@/components/ui/button'

export default function SemPermissao() {
  return (
    <main className="flex min-h-svh flex-col items-center justify-center gap-4 p-6 text-center">
      <h1 className="text-xl font-semibold">Sem permissão</h1>
      <p className="text-muted-foreground max-w-sm text-sm">
        Seu perfil não tem acesso a esta área do sistema.
      </p>
      <Button asChild variant="outline">
        <Link to="/">Voltar ao início</Link>
      </Button>
    </main>
  )
}
