package br.edu.nutriclinica.service.secao;

import org.openapitools.jackson.nullable.JsonNullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * As duas semânticas de escrita das seções do prontuário, num lugar só.
 *
 * <p>{@link #aplicar} é o PATCH: campo ausente mantém, campo presente com
 * {@code null} limpa. {@link #reconciliar} é o PUT: a lista enviada passa a ser
 * a lista inteira.
 */
public final class Merge {

    private Merge() {
    }

    /**
     * Aplica um campo de PATCH ao destino.
     *
     * <p>Ausente no JSON chega aqui como {@code null} — o Jackson não instancia o
     * container para uma chave que não veio. Enviado como {@code null} chega como
     * {@code JsonNullable.of(null)}, presente, e limpa o campo. Tratar os dois
     * como a mesma coisa faria o autosave do frontend apagar o que a tela não
     * enviou, que é justamente o que esta distinção evita.
     */
    public static <T> void aplicar(JsonNullable<T> valor, Consumer<T> destino) {
        if (valor != null && valor.isPresent()) {
            destino.accept(valor.get());
        }
    }

    /**
     * Substitui uma coleção do prontuário pela lista enviada, preservando os ids
     * dos itens que continuaram.
     *
     * <p>A coleção é mutada no lugar, não trocada: quem chama passa a própria
     * lista da entidade, mapeada com {@code orphanRemoval = true}, e é o
     * Hibernate que emite os DELETE do que saiu. Só os ausentes são removidos —
     * os que ficaram continuam sendo a mesma linha, com o mesmo id, para que um
     * comentário do supervisor preso a um item não perca a âncora quando o
     * estagiário edita outro.
     *
     * <p>Um id que veio no corpo mas não está nesta coleção é tratado como
     * desconhecido e vira registro novo. É o que impede um estagiário de
     * sequestrar a linha de outro atendimento mandando o id dela.
     *
     * @param atuais   coleção gerenciada da entidade, mutada no lugar
     * @param enviados o que veio no corpo, na ordem em que veio
     * @param idAtual  id de um registro já gravado
     * @param idDe     id declarado no corpo, ou {@code null} para registro novo
     * @param criar    fábrica de um registro novo, já vinculado ao atendimento
     * @param copiar   copia o corpo para a entidade, ciente da posição na lista
     * @return as entidades na ordem em que o cliente as enviou
     */
    public static <E, R> List<E> reconciliar(List<E> atuais,
                                             List<R> enviados,
                                             Function<E, Long> idAtual,
                                             Function<R, Long> idDe,
                                             Supplier<E> criar,
                                             Copiador<R, E> copiar) {
        Map<Long, E> porId = new HashMap<>();
        for (E atual : atuais) {
            Long id = idAtual.apply(atual);
            if (id != null) {
                porId.put(id, atual);
            }
        }

        List<E> resultado = new ArrayList<>(enviados.size());
        List<E> novos = new ArrayList<>();

        for (int posicao = 0; posicao < enviados.size(); posicao++) {
            R enviado = enviados.get(posicao);
            Long id = idDe.apply(enviado);

            // Sair do índice significa "continua na lista": o que restar nele ao
            // fim do laço é exatamente o que o cliente não mandou de volta.
            E entidade = id == null ? null : porId.remove(id);
            if (entidade == null) {
                entidade = criar.get();
                novos.add(entidade);
            }
            copiar.copiar(enviado, entidade, posicao);
            resultado.add(entidade);
        }

        // As entidades não têm equals próprio, então removeAll compara por
        // identidade — que é o que se quer aqui.
        atuais.removeAll(new ArrayList<>(porId.values()));
        atuais.addAll(novos);

        return resultado;
    }

    /** Copia o que veio no corpo para a entidade, ciente da posição na lista enviada. */
    @FunctionalInterface
    public interface Copiador<R, E> {
        void copiar(R enviado, E entidade, int posicao);
    }

    /**
     * A ordem que vai para o banco: a enviada quando o cliente se importou com
     * ela, senão a posição no array — que é a ordem em que a tela mostrou.
     */
    public static int ordem(Integer enviada, int posicao) {
        return enviada != null ? enviada : posicao;
    }
}
