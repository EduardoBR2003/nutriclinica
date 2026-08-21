import { useCallback, useMemo, useRef, useState, type ReactNode } from 'react'

import { ToastContext, type Toast } from '@/lib/toastContext'

const DURACAO_MS = 4200

/**
 * Toast do design: card escuro fixo em baixo-direita, some sozinho.
 * Implementação local em vez de mais uma dependência — é uma tela só de UI.
 */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [avisos, setAvisos] = useState<Toast[]>([])
  const proximoId = useRef(1)

  const avisar = useCallback((titulo: string, texto?: string) => {
    const id = proximoId.current++
    setAvisos((atuais) => [...atuais, { id, titulo, texto }])
    setTimeout(() => {
      setAvisos((atuais) => atuais.filter((a) => a.id !== id))
    }, DURACAO_MS)
  }, [])

  const valor = useMemo(() => ({ avisar }), [avisar])

  return (
    <ToastContext value={valor}>
      {children}
      <div className="pointer-events-none fixed right-[22px] bottom-[22px] z-60 flex flex-col gap-2.5">
        {avisos.map((aviso) => (
          <div
            key={aviso.id}
            role="status"
            className="bg-inverse shadow-toast max-w-[340px] rounded-2xl px-4 py-3.5"
          >
            <p className="text-md font-bold text-white">{aviso.titulo}</p>
            {aviso.texto ? (
              <p className="text-on-inverse mt-0.5 text-base leading-normal">{aviso.texto}</p>
            ) : null}
          </div>
        ))}
      </div>
    </ToastContext>
  )
}
