/**
 * Normaliza o formulário para o corpo do `PATCH`.
 *
 * O contrato usa `JsonNullable`: campo ausente mantém o valor, `null` limpa.
 * Um input esvaziado devolve `''`, que precisa virar `null` para de fato apagar
 * o dado no servidor — senão o usuário limpa a tela e nada muda no prontuário.
 */
export function paraPatch<T extends Record<string, unknown>>(valores: T): T {
  const saida: Record<string, unknown> = {}
  for (const [chave, valor] of Object.entries(valores)) {
    saida[chave] =
      valor === '' || (typeof valor === 'number' && Number.isNaN(valor)) ? null : valor
  }
  return saida as T
}

/** Troca `null`/`undefined` por `''` para preencher inputs controlados. */
export function paraFormulario<T extends Record<string, unknown>>(
  valores: T | undefined,
  padroes: T,
): T {
  const saida: Record<string, unknown> = { ...padroes }
  if (!valores) return saida as T
  for (const [chave, valor] of Object.entries(valores)) {
    if (!(chave in padroes)) continue
    saida[chave] = valor === null || valor === undefined ? padroes[chave] : valor
  }
  return saida as T
}
