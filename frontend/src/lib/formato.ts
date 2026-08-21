/** Formatação de exibição — nada aqui volta para a API. */

/** `2026-08-14` → `14/08/2026`. Aceita também `date-time` ISO. */
export function formatarData(iso: string | undefined | null): string {
  if (!iso) return '—'
  const [data] = iso.split('T')
  const partes = data?.split('-')
  if (!partes || partes.length !== 3) return iso
  return `${partes[2]}/${partes[1]}/${partes[0]}`
}

/** `2026-08-14T09:02:31` → `14/08/2026 09:02`. */
export function formatarDataHora(iso: string | undefined | null): string {
  if (!iso) return '—'
  const [data, hora] = iso.split('T')
  if (!hora) return formatarData(data)
  return `${formatarData(data)} ${hora.slice(0, 5)}`
}

/** Número com vírgula decimal, como se escreve em prontuário. */
export function formatarNumero(
  valor: number | undefined | null,
  casas = 1,
): string {
  if (valor === undefined || valor === null || Number.isNaN(valor)) return '—'
  return valor.toFixed(casas).replace('.', ',')
}

/** Iniciais para o avatar: duas letras, ignorando preposições. */
export function iniciais(nome: string | undefined | null): string {
  const partes = String(nome ?? '')
    .trim()
    .split(/\s+/)
    .filter((p) => p.length > 2)
  const letras = partes.slice(0, 2).map((p) => p[0]?.toUpperCase() ?? '')
  return letras.join('') || '—'
}

/** Data de hoje em `AAAA-MM-DD`, para `defaultValue` de `<input type="date">`. */
export function hojeIso(): string {
  const agora = new Date()
  const mes = String(agora.getMonth() + 1).padStart(2, '0')
  const dia = String(agora.getDate()).padStart(2, '0')
  return `${agora.getFullYear()}-${mes}-${dia}`
}
