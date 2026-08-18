import { Link } from 'react-router'

import { Button } from '@/components/ui/button'

export default function NaoEncontrada() {
  return (
    <main className="flex min-h-svh flex-col items-center justify-center gap-4 p-6 text-center">
      <h1 className="text-xl font-semibold">Página não encontrada</h1>
      <p className="text-muted-foreground text-sm">O endereço acessado não existe.</p>
      <Button asChild variant="outline">
        <Link to="/">Voltar ao início</Link>
      </Button>
    </main>
  )
}
