import { createContext } from 'react'

export interface BuscaContextValue {
  termo: string
  definirTermo: (termo: string) => void
  /** `null` esconde o campo — a busca do design é contextual, não global. */
  placeholder: string | null
  registrar: (placeholder: string) => void
  desregistrar: () => void
}

export const BuscaContext = createContext<BuscaContextValue | null>(null)
