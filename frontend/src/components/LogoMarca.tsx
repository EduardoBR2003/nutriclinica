import { cn } from '@/lib/utils'

interface LogoMarcaProps {
  className?: string
}

/**
 * Recorte quadrado do símbolo — só o prato, sem os talheres — para os lugares
 * apertados da interface (sidebar) e para o ícone da aba. Os traços vêm
 * engrossados e as nervuras da folha ficaram de fora: nesse tamanho os pesos
 * originais viram menos de um pixel e o desenho embola. É o mesmo recorte de
 * `public/favicon.svg`, então a aba e a sidebar mostram a mesma coisa.
 */
export function LogoMarca({ className }: LogoMarcaProps) {
  return (
    <svg
      viewBox="244 54 192 192"
      aria-hidden
      focusable="false"
      className={cn('size-9 shrink-0', className)}
    >
      <circle cx="340" cy="150" r="86" fill="none" stroke="#639922" strokeWidth="8" />

      <path
        d="M340 88 C376 112 382 146 340 172 C298 146 304 112 340 88 Z"
        fill="#97C459"
        stroke="#4F8A1D"
        strokeWidth="5"
        strokeLinejoin="round"
      />
      <path
        d="M340 170 V94"
        fill="none"
        stroke="#2F5A0E"
        strokeWidth="4.5"
        strokeLinecap="round"
        opacity="0.75"
      />

      <path
        d="M288 196 H310 L320 178 L332 214 L344 189 L353 196 H392"
        fill="none"
        stroke="#639922"
        strokeWidth="9"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}
