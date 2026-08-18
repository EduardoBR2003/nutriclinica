package br.edu.nutriclinica.service.auditoria;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca uma operação que deixa rastro em {@code log_auditoria}.
 *
 * <p>São dados sensíveis de saúde: a LGPD exige saber quem submeteu, quem
 * avaliou e quem <b>leu</b> cada prontuário. A anotação existe para que esse
 * registro não seja uma chamada copiada em cada ponto — quem lê o método vê a
 * marca e o {@link AuditoriaAspect} cuida do resto: usuário autenticado,
 * endereço de origem, instante e id da entidade.
 *
 * <p><b>Convenção do id:</b> o aspecto usa o primeiro argumento {@code Long} do
 * método como {@code entidade_id}. Os métodos auditados recebem o id do
 * atendimento como primeiro parâmetro; um método sem argumento {@code Long}
 * ainda é auditado, só que sem id.
 *
 * <p>Vale só em métodos alcançados pelo proxy do Spring — chamada interna, de um
 * método da mesma classe, não passa pelo aspecto e não audita.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Auditavel {

    /** Verbo registrado na coluna {@code acao}, de no máximo 30 caracteres. */
    String acao();

    /** Tipo do alvo, na coluna {@code entidade}, de no máximo 60 caracteres. */
    String entidade();
}
