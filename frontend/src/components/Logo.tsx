import { cn } from '@/lib/utils'

interface LogoProps {
  className?: string
}

/**
 * Símbolo completo — prato, folha, linha de pulso e talheres. Fica nas telas de
 * entrada, onde há espaço para os talheres respirarem; no enxuto da aplicação
 * use a [LogoMarca], que é o mesmo prato recortado em quadrado.
 *
 * `aria-hidden` porque a marca nunca aparece sozinha: vem sempre colada ao
 * texto "NutriClinica", e um nome acessível aqui faria o leitor de tela repetir.
 */
export function Logo({ className }: LogoProps) {
  return (
    <svg
      viewBox="182 45 320 210"
      aria-hidden
      focusable="false"
      className={cn('h-10 w-auto shrink-0', className)}
    >
      <circle cx="340" cy="150" r="88" fill="none" stroke="#639922" strokeWidth="2.5" />
      <circle cx="340" cy="150" r="78" fill="none" stroke="#639922" strokeWidth="0.8" opacity="0.5" />

      <path
        d="M340 88 C376 112 382 146 340 172 C298 146 304 112 340 88 Z"
        fill="#97C459"
        stroke="#4F8A1D"
        strokeWidth="1.6"
        strokeLinejoin="round"
      />
      <path
        d="M340 170 V94"
        fill="none"
        stroke="#2F5A0E"
        strokeWidth="1.4"
        strokeLinecap="round"
        opacity="0.75"
      />
      <path
        d="M340 140 L322 124 M340 156 L358 140 M340 124 L326 112"
        fill="none"
        stroke="#2F5A0E"
        strokeWidth="1.1"
        strokeLinecap="round"
        opacity="0.6"
      />

      <path
        d="M272 195 H310 L320 176 L332 214 L344 188 L354 195 H408"
        fill="none"
        stroke="#639922"
        strokeWidth="2.6"
        strokeLinecap="round"
        strokeLinejoin="round"
      />

      <path
        d="M200 96 V126 Q200 144 208 149 V206 Q208 214 212 214 Q216 214 216 206 V149 Q224 144 224 126 V96"
        fill="none"
        stroke="#639922"
        strokeWidth="2.2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path
        d="M208 96 V128 M216 96 V128"
        fill="none"
        stroke="#639922"
        strokeWidth="2.2"
        strokeLinecap="round"
      />

      <ellipse cx="470" cy="119" rx="15" ry="25" fill="none" stroke="#639922" strokeWidth="2.2" />
      <path d="M470 144 V212" fill="none" stroke="#639922" strokeWidth="2.2" strokeLinecap="round" />
    </svg>
  )
}
