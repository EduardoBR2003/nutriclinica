import { Botao } from '@/components/Botao'

interface RodapeSalvarProps {
  visivel: boolean
  salvando: boolean
  sujo: boolean
}

/**
 * O design não tinha botão de salvar (o protótipo guardava tudo em memória).
 * Contra a API real cada seção precisa do seu `PATCH`/`PUT` explícito.
 */
export function RodapeSalvar({ visivel, salvando, sujo }: RodapeSalvarProps) {
  if (!visivel) return null
  return (
    <div className="border-line-subtle mt-4 flex items-center justify-end gap-3 border-t pt-4">
      {sujo ? (
        <span className="text-cta-text text-base font-medium">Alterações não salvas</span>
      ) : null}
      <Botao type="submit" disabled={salvando}>
        {salvando ? 'Salvando…' : 'Salvar seção'}
      </Botao>
    </div>
  )
}
