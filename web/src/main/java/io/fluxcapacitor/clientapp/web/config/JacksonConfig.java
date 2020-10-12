package io.fluxcapacitor.clientapp.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import io.fluxcapacitor.javaclient.common.serialization.jackson.JacksonSerializer;

import javax.ws.rs.ext.ContextResolver;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT;

public class JacksonConfig implements ContextResolver<ObjectMapper> {
    public static final ObjectMapper mapper = JacksonSerializer.defaultObjectMapper.rebuild()
            .activateDefaultTyping(LaissezFaireSubTypeValidator.instance, JAVA_LANG_OBJECT, PROPERTY)
            .build();

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }
}
