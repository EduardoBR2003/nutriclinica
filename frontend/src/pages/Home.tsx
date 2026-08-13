import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'

/**
 * Página provisória da Etapa 0 — serve apenas para validar o setup
 * (Vite + Tailwind + shadcn/ui + Router). As telas reais vêm na Etapa 1.
 */
export default function Home() {
  return (
    <main className="flex min-h-svh items-center justify-center p-6">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>NutriClinica</CardTitle>
          <CardDescription>
            Sistema de prontuário da clínica escola de nutrição
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="text-muted-foreground text-sm">
            Etapa 0 — fundação do monorepo. As telas serão implementadas na Etapa 1.
          </p>
          <Button asChild variant="outline">
            <a href={`${import.meta.env.VITE_API_URL ?? ''}/swagger-ui.html`}>
              Abrir Swagger da API
            </a>
          </Button>
        </CardContent>
      </Card>
    </main>
  )
}
