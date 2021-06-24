package io.fluxcapacitor.clientapp.common.logging;

import io.fluxcapacitor.common.MessageType;
import io.fluxcapacitor.common.api.SerializedMessage;
import io.fluxcapacitor.common.handling.Handler;
import io.fluxcapacitor.javaclient.common.Message;
import io.fluxcapacitor.javaclient.common.serialization.DeserializingMessage;
import io.fluxcapacitor.javaclient.publishing.DispatchInterceptor;
import io.fluxcapacitor.javaclient.tracking.handling.HandlerInterceptor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

@Slf4j
public enum LoggingInterceptor implements HandlerInterceptor, DispatchInterceptor {
    instance;

    @Override
    public Function<Message, SerializedMessage> interceptDispatch(Function<Message, SerializedMessage> function,
                                                                  MessageType messageType) {
        return m -> {
            if (MessageType.METRICS != messageType) {
                log.info("Sending {} {}", messageType.name().toLowerCase(), m.getPayload());
            }
            return function.apply(m);
        };
    }

    @Override
    public Function<DeserializingMessage, Object> interceptHandling(Function<DeserializingMessage, Object> function,
                                                                    Handler<DeserializingMessage> handler,
                                                                    String consumer) {
        return m -> {
            if (MessageType.METRICS != m.getMessageType()) {
                log.info("Handling a {} {} in {}", m.getPayloadClass().getSimpleName(),
                         m.getMessageType().name().toLowerCase(), handler);
            }
            return function.apply(m);
        };
    }
}
