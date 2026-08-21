import { useContext, useEffect, useState } from 'react'

import { BuscaContext, type BuscaContextValue } from '@/lib/buscaContext'

function useBuscaContexto(): BuscaContextValue {
  const contexto = useContext(BuscaContext)
  if (!contexto) throw new Error('useBusca precisa estar dentro de <LayoutApp>.')
  return contexto
}

/** Usado pelo cabeçalho para desenhar (ou esconder) o campo de busca. */
export function useBuscaHeaderControle() {
  return useBuscaContexto()
}

/**
 * Liga a busca do cabeçalho a uma lista. A página declara o placeholder e
 * recebe o termo já debounced, pronto para virar query param.
 */
export function useBuscaHeader(placeholder: string, atrasoMs = 300): string {
  const { termo, registrar, desregistrar } = useBuscaContexto()
  const [debounced, setDebounced] = useState(termo)

  useEffect(() => {
    registrar(placeholder)
    return desregistrar
  }, [placeholder, registrar, desregistrar])

  useEffect(() => {
    const id = setTimeout(() => setDebounced(termo), atrasoMs)
    return () => clearTimeout(id)
  }, [termo, atrasoMs])

  return debounced
}
