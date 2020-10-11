package io.fluxcapacitor.clientapp.adapter.documentation;

import com.fasterxml.jackson.databind.JavaType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.lang.annotation.Annotation;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

@Slf4j
public class SwaggerModelConverter implements ModelConverter {

    @SuppressWarnings({"rawtypes"})
    @Override
    public Schema resolve(AnnotatedType annotatedType, ModelConverterContext context, Iterator<ModelConverter> chain) {
        if (chain.hasNext()) {
            ModelResolver next = (ModelResolver) chain.next();
            Schema schema = next.resolve(annotatedType, context, chain);
            if (annotatedType.isSchemaProperty()) {
                schema = processSchemaProperty(schema, annotatedType);
            }
            return schema;
        } else {
            return null;
        }
    }

    @SuppressWarnings({"rawtypes"})
    private Schema processSchemaProperty(Schema schema, AnnotatedType annotatedType) {
        JavaType type = Json.mapper().constructType(annotatedType.getType());
        if (type == null) {
            return schema;
        }
        schema = composeSchema(schema, annotatedType, type);
        Class<?> cls = type.getRawClass();
        if (Instant.class.isAssignableFrom(cls)) {
            schema.format("date-time").type("string");
        } else if (LocalTime.class.isAssignableFrom(cls)) {
            schema.format("partial-time").type("string");
        }
        if (!type.isPrimitive() && !Collection.class.isAssignableFrom(cls)
                && !Map.class.isAssignableFrom(cls) && !isRequired(annotatedType)) {
            schema.nullable(true);
        }
        return schema;
    }

    @SuppressWarnings({"rawtypes"})
    private Schema composeSchema(Schema schema, AnnotatedType annotatedType, JavaType type) {
        if (!"object".equals(schema.getType()) && schema.get$ref() == null) {
            return schema;
        }
        ComposedSchema result = new ComposedSchema();
        Schema ref = schema;
        if (ref.get$ref() == null) {
            ref = new ObjectSchema();
            ref.$ref("#/components/schemas/" + type.getRawClass().getSimpleName());
        }
        result.addAllOfItem(ref);

        Annotation[] ctxAnnotations = annotatedType.getCtxAnnotations();
        if (ctxAnnotations != null) {
            Arrays.stream(ctxAnnotations).filter(a -> a instanceof io.swagger.v3.oas.annotations.media.Schema)
                    .map(s -> (io.swagger.v3.oas.annotations.media.Schema) s).findFirst().ifPresent(s -> {
                result.description(s.description());
                result.deprecated(s.deprecated());
                result.example(s.example());
                result.title(s.title());
            });
        }
        return result;
    }

    private boolean isRequired(AnnotatedType annotatedType) {
        if (annotatedType.getCtxAnnotations() == null) {
            return false;
        }
        return Arrays.stream(annotatedType.getCtxAnnotations())
                .anyMatch(annotation -> annotation instanceof NotBlank || annotation instanceof NotNull);
    }
}
