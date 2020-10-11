package io.fluxcapacitor.clientapp.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import io.fluxcapacitor.common.api.Metadata;
import io.fluxcapacitor.javaclient.FluxCapacitor;
import io.fluxcapacitor.javaclient.common.serialization.DeserializingMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

@Slf4j
public class FluxCapacitorLogbackAppender extends AppenderBase<ILoggingEvent> {

    private static final int MAX_ERROR_SIZE = 1024 * 1024; // max error size = 1MB

    @Override
    protected void append(ILoggingEvent event) {
        try {
            Optional<Throwable> throwable =
                    ofNullable((ThrowableProxy) event.getThrowableProxy()).map(ThrowableProxy::getThrowable);
            Metadata metadata = Metadata.from(ofNullable(DeserializingMessage.getCurrent()).<Map<String, String>>map(
                    d -> d.getSerializedObject().getMetadata()).orElse(emptyMap()));
            metadata.putAll(Map.of("stackTrace", format("[%s] %s %s - %s%s", event.getThreadName(), event.getLevel(),
                                         event.getLoggerName(), event.getFormattedMessage(),
                                         throwable.map(e -> "\n" + getStackTrace(e)).orElse("")),
                    "level", event.getLevel().toString()));
            throwable.ifPresentOrElse(e -> {
                metadata.put("error", e.getClass().getSimpleName());
                metadata.put("errorMessage", isBlank(e.getMessage()) ? event.getFormattedMessage() : e.getMessage());
                Optional.ofNullable(e.getStackTrace()).filter(s -> s.length > 0)
                        .ifPresent(s -> metadata.put("traceElement", s[0].toString()));
            }, () -> metadata.put("errorMessage", event.getFormattedMessage()));

            FluxCapacitor fluxCapacitor;
            try {
                fluxCapacitor = FluxCapacitor.get();
            } catch (Throwable e) {
                log.info("Not logging error to Flux Capacitor: {}", e.getMessage());
                return;
            }
            if (event.getFormattedMessage().getBytes().length > MAX_ERROR_SIZE) {
                log.warn("Not logging error to Flux Capacitor: error is larger than max error size");
                return;
            }
            fluxCapacitor.errorGateway().report(new ConsoleError(), metadata);
        } catch (Throwable e) {
            log.info("Failed to publish console error", e);
        }
    }

}
