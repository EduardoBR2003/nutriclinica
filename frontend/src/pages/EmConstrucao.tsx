/**
 * Marcador das rotas que ainda não têm tela. O Bloco F1 entrega só a fundação
 * (sessão, rotas, erros) e o login; cada uma destas vira uma página de verdade
 * nos blocos seguintes.
 */
export default function EmConstrucao({ titulo }: { titulo: string }) {
  return (
    <section>
      <h1 className="text-xl font-semibold">{titulo}</h1>
      <p className="text-muted-foreground mt-2 text-sm">
        Tela ainda não implementada — chega em um próximo bloco da Etapa 4.
      </p>
    </section>
  )
}
