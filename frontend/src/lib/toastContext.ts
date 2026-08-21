import { createContext } from 'react'

export interface Toast {
  id: number
  titulo: string
  texto?: string
}

export interface ToastContextValue {
  /** Exibe um aviso efêmero no canto inferior direito. */
  avisar: (titulo: string, texto?: string) => void
}

export const ToastContext = createContext<ToastContextValue | null>(null)
