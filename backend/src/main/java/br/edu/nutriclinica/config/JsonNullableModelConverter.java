package br.edu.nutriclinica.config;

import com.fasterxml.jackson.databind.JavaType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import org.openapitools.jackson.nullable.JsonNullable;

import java.util.Iterator;

/**
 * Faz o {@code JsonNullable<T>} aparecer na documentação como {@code T}.
 *
 * <p>Sem isto, o springdoc publica um schema {@code JsonNullableString} para
 * cada campo de PATCH, e o Swagger passa a descrever uma API que não existe: o
 * corpo aceito continua sendo {@code {"motivo": "texto"}}, não um objeto
 * embrulhado. Como {@code docs/api.yaml} é a fonte da verdade e descreve os
 * campos como tipos simples, o que o servidor publica precisa concordar com ele.
 *
 * <p>O que se perde no caminho é a única coisa que o schema não sabe expressar
 * mesmo: que <i>ausente</i> e <i>null</i> significam coisas diferentes. Isso
 * está descrito na documentação das próprias rotas.
 */
public class JsonNullableModelConverter implements ModelConverter {

    @Override
    public Schema<?> resolve(AnnotatedType tipo,
                             ModelConverterContext contexto,
                             Iterator<ModelConverter> proximos) {
        JavaType javaType = Json.mapper().constructType(tipo.getType());

        if (javaType != null && JsonNullable.class.isAssignableFrom(javaType.getRawClass())) {
            JavaType interno = javaType.containedType(0);
            if (interno != null) {
                // As anotações de contexto seguem junto: é delas que saem as
                // faixas de validação (minimum, maximum) no schema publicado.
                return contexto.resolve(new AnnotatedType(interno)
                        .jsonViewAnnotation(tipo.getJsonViewAnnotation())
                        .ctxAnnotations(tipo.getCtxAnnotations())
                        .schemaProperty(tipo.isSchemaProperty())
                        .resolveAsRef(tipo.isResolveAsRef()));
            }
        }

        return proximos.hasNext() ? proximos.next().resolve(tipo, contexto, proximos) : null;
    }
}
