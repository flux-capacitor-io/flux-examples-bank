package com.flowmaps.auditlog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flowmaps.auditlog.OpenSearch.Operation;
import io.fluxcapacitor.common.MessageType;
import io.fluxcapacitor.common.Registration;
import io.fluxcapacitor.common.api.Metadata;
import io.fluxcapacitor.common.api.SerializedMessage;
import io.fluxcapacitor.common.api.eventsourcing.AppendEvents;
import io.fluxcapacitor.common.api.eventsourcing.EventBatch;
import io.fluxcapacitor.common.serialization.JsonUtils;
import io.fluxcapacitor.javaclient.FluxCapacitor;
import io.fluxcapacitor.javaclient.common.Message;
import io.fluxcapacitor.javaclient.common.serialization.Serializer;
import io.fluxcapacitor.javaclient.tracking.ConsumerConfiguration;
import io.fluxcapacitor.javaclient.tracking.ErrorHandler;
import io.fluxcapacitor.javaclient.tracking.IndexUtils;
import io.fluxcapacitor.javaclient.tracking.RetryingErrorHandler;
import io.fluxcapacitor.javaclient.tracking.client.DefaultTracker;
import io.fluxcapacitor.javaclient.web.WebRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static com.flowmaps.auditlog.OpenSearch.Operation.Action.create;
import static io.fluxcapacitor.common.MessageType.ERROR;
import static io.fluxcapacitor.common.MessageType.METRICS;
import static io.fluxcapacitor.common.MessageType.SCHEDULE;
import static io.fluxcapacitor.javaclient.tracking.IndexUtils.millisFromIndex;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;

@Component
@Slf4j
public class OpenSearchPublisher implements InitializingBean, DisposableBean {

    private static final ConcurrentHashMap<String, List<String>> aliases = new ConcurrentHashMap<>();

    private static final EnumSet<MessageType> trackedMessageTypes = EnumSet.allOf(MessageType.class);
    private static final int MAX_PAYLOAD_SIZE = 16_000;

    private final OpenSearch openSearch;
    private Registration registration;
    private final FluxCapacitor fluxCapacitor;
    private final Serializer serializer;

    @Autowired
    public OpenSearchPublisher(OpenSearch openSearch, FluxCapacitor fluxCapacitor, Serializer serializer) {
        this.openSearch = openSearch;
        this.fluxCapacitor = fluxCapacitor;
        this.serializer = serializer;
    }

    @Override
    public void afterPropertiesSet() {
        this.openSearch.initialize();
        this.registration = trackedMessageTypes.stream()
                .filter(m -> ElasticIndex.of(m) != null) //
                .map(this::startTracking).reduce(Registration::merge).orElse(Registration.noOp());
    }

    private Registration startTracking(MessageType messageType) {
        ConsumerConfiguration configuration = ConsumerConfiguration.builder()
                .minIndex(0L)
                .name(format("%s:%s", fluxCapacitor.client().name(), messageType.name()))
                .maxFetchSize(messageType == METRICS ? 8192 : 128)
                .errorHandler(new RetryingErrorHandler())
                .build();

        log.info("Starting tracker {}", configuration.getName());
        Consumer<List<SerializedMessage>> consumer = messages -> {
            try {
                handle(messages, messageType);
            } catch (Throwable e) {
                handleError(configuration.getErrorHandler(), configuration.getName(),
                            () -> handle(messages, messageType), e);
            }
        };
        return DefaultTracker.start(consumer, messageType, configuration, fluxCapacitor.client());
    }

    @SneakyThrows
    private void handleError(ErrorHandler errorHandler, String trackerName, Runnable handle, Throwable e) {
        errorHandler.handleError(e, format("Handler %s failed to handle messages", trackerName), handle);
    }

    @SneakyThrows
    private void handle(List<SerializedMessage> messages, MessageType messageType) {
        if (!messages.isEmpty()) {
            var type = ofNullable(ElasticIndex.of(messageType)).orElseThrow();
            var aliasName = type.getAlias().name();
            var indices = new ArrayList<>(ofNullable(aliases.get(aliasName))
                                                  .orElseGet(() -> aliases.compute(aliasName,
                                                                                   (k, v) -> List.of(aliasName))));

            var operations = new ArrayList<Operation>();

            messages.forEach(m -> {
                var esIndex = type.getEsIndex();
                if (!indices.contains(esIndex.getIndex())) {
                    if (true) {
                        indices.add(esIndex.getIndex());
                    }
                    aliases.put(aliasName, indices);
                }
                operations.add(new Operation(create, esIndex.getIndex(), messageType.name() + m.getIndex(),
                                             createEntry(messageType, m)));
            });
            var errors = openSearch.bulk(operations).stream().filter(Objects::nonNull).toList();
            if (!errors.isEmpty()) {
                if (messageType != ERROR) {
                    log.error("Failed to insert {} batch ({}/{} errored, first message: {})", messageType,
                              errors.size(), messages.size(), describe(messages.get(0)), errors.get(0));
                    throw errors.get(0);
                } else {
                    log.error("Failed to insert {} batch ({}/{} errored, first message: {}). Continuing...\nReason: {}",
                              messageType,
                              errors.size(), messages.size(), describe(messages.get(0)), errors.get(0).getMessage());
                }
            }
        }
    }

    private String describe(SerializedMessage message) {
        return String.format("Index= %s", message.getIndex());
    }

    private Map<String, Object> createEntry(MessageType messageType, SerializedMessage m) {
        Map<String, Object> value = new LinkedHashMap<>();
        String type = m.getData().getType();
        value.put("messageType", messageType.name());
        value.put("messageId", m.getMessageId());
        value.put("type", m.getData().getType());
        value.put("revision", m.getData().getRevision());
        value.put("segment", m.getSegment());
        value.put("index", m.getIndex());
        value.put("messageIndex", m.getIndex().toString());
        value.put("source", m.getSource());
        value.put("target", m.getTarget());
        value.put("requestId", m.getRequestId());
        value.put("@timestamp", Instant.ofEpochMilli(messageType == SCHEDULE ? millisFromIndex(m.getIndex()) : m.getTimestamp()).toString());

         {
            String payload = new String(m.getData().getValue());
            if (payload.length() > MAX_PAYLOAD_SIZE) {
                payload = payload.substring(0, MAX_PAYLOAD_SIZE) + "...";
            }
            value.put("payload", payload);
        }

        switch (messageType) {
            case WEBREQUEST -> {
                Metadata metadata = m.getMetadata();
                if ("application/json".equals(m.getData().getFormat())) {
                    var event = JsonUtils.fromJson(m.getData().getValue(), ObjectNode.class);
                    if (event != null) {
                        if (Message.class.getName().equals(event.path("@class").textValue())) {
                            ObjectNode payload = (ObjectNode) event.path("payload");
                            payload.remove("password");
                            payload.remove("recoveryCode");
                            value.put("type", payload.path("@class").textValue());
                            value.put("payload", JsonUtils.asJson(payload));
                            JsonNode extraMetadata = event.path("metadata");
                            if (extraMetadata != null) {
                                var converted = JsonUtils.convertValue(extraMetadata, Metadata.class);
                                if (converted != null) {
                                    metadata = metadata.with(converted);
                                }
                            }
                        } else {
                            value.put("type", event.path("@class").textValue());
                            event.remove("password");
                            event.remove("recoveryCode");
                            value.put("payload", JsonUtils.asJson(event));
                        }
                    }
                }
                Map<String, List<String>> headers = new LinkedHashMap<>(WebRequest.getHeaders(m.getMetadata()));
                List<String> scrambledHeader = List.of("<value scrambled>");
                headers.computeIfPresent("Authorization", (k, v) -> scrambledHeader);
                headers.computeIfPresent("Cookie", (k, v) -> scrambledHeader);
                value.put("metadata", metadata.with("headers", headers));
            }
            case WEBRESPONSE -> {
                Map<String, List<String>> headers = new LinkedHashMap<>(WebRequest.getHeaders(m.getMetadata()));
                List<String> scrambledHeader = List.of("<value scrambled>");
                headers.computeIfPresent("Set-Cookie", (k, v) -> scrambledHeader);
                value.put("metadata", m.getMetadata().with("headers", headers));
            }
            case METRICS -> {
                Long msDuration = ofNullable(m.getMetadata().getOrDefault("msDuration", null))
                        .map(Long::parseLong).orElse(null);
                ofNullable(msDuration).ifPresent(d -> value.put("millisecondDuration", d));
                value.put("metadata", m.getMetadata().without("msDuration"));
                try {
                    switch (type) {
                        case "io.fluxcapacitor.common.api.eventsourcing.AppendEvents$Metric" -> {
                            AppendEvents.Metric event = serializer.deserialize(m.getData());
                            value.put("size", event.getEventBatches().stream().map(EventBatch.Metric::getSize)
                                    .reduce(Integer::sum));

                        }
                        case "io.fluxcapacitor.common.api.eventsourcing.GetEvents" -> {

                        }
                        default -> {
                            JsonNode metric = JsonUtils.readTree(m.getData().getValue());
                            ofNullable(metric.get("size"))
                                    .or(() -> ofNullable(metric.get("batchSize")))
                                    .or(() -> ofNullable(metric.path("eventBatch").get("size")))
                                    .or(() -> ofNullable(metric.path("messageBatch").get("size")))
                                    .ifPresent(s -> value.put("size", s.asLong()));
                            ofNullable(metric.get("lastIndex"))
                                    .or(() -> ofNullable(metric.path("messageBatch").get("lastIndex")))
                                    .map(JsonNode::asLong)
                                    .map(IndexUtils::timestampFromIndex)
                                    .map(ts -> ChronoUnit.MILLIS.between(ts, Instant.ofEpochMilli(m.getTimestamp())))
                                    .ifPresent(s -> value.put("millisecondsBehind", s));
                            ofNullable(metric.get("handler")).ifPresent(s -> value.put("handler", s.asText()));
                            ofNullable(metric.get("payloadType")).ifPresent(s -> value.put("payloadType", s.asText()));
                            ofNullable(metric.get("nanosecondDuration")).ifPresent(
                                    s -> value.put("millisecondDuration", Math.round((double) s.asLong() / 1_000_000)));
                            if (msDuration != null) {
                                ofNullable(metric.get("timestamp")).ifPresent(
                                        ts -> value.put("serverMsDuration",
                                                        ts.asLong() - (m.getTimestamp() - msDuration)));
                            }
                            ofNullable(m.getMetadata().get("serverMsDuration")).ifPresent(
                                    duration -> value.put("serverMsDuration", duration));
                            ofNullable(metric.get("segment")).or(
                                            () -> ofNullable(metric.path("messageBatch").get("segment")))
                                    .ifPresent(s -> value.put("segmentRange", s));
                        }
                    }
                } catch (Exception ex) {
                    log.error(format("Something went wrong retrieving metadata from %s event", type), ex);
                }
            }
            default -> value.put("metadata", m.getMetadata().without("headers"));
        }

        return value;
    }

    @Getter
    @AllArgsConstructor
    enum ElasticAlias {
        eventlog(Period.ofMonths(12)),
        querylog(Period.ofMonths(1)),
        weblog(Period.ofMonths(1)),
        metricslog(Period.ofDays(7));

        private final Period retentionTime;
    }

    @Getter
    @AllArgsConstructor
    enum ElasticIndex {
        COMMAND(ElasticAlias.eventlog),
        EVENT(ElasticAlias.eventlog),
        ERROR(ElasticAlias.eventlog),
        SCHEDULE(ElasticAlias.eventlog),
        QUERY(ElasticAlias.querylog),
        RESULT(ElasticAlias.querylog),
        WEBREQUEST(ElasticAlias.weblog),
        WEBRESPONSE(ElasticAlias.weblog),
        METRICS(ElasticAlias.metricslog);

        private final ElasticAlias alias;


        public String getIndexName() {
            return this.name().toLowerCase() + "log";
        }

        public EsIndex getEsIndex() {
            return new EsIndex(getAlias().name(), getAlias().name());
        }

        public static ElasticIndex of(MessageType messageType) {
            try {
                return ElasticIndex.valueOf(messageType.name());
            } catch (Exception e) {
                return null;
            }
        }
    }

    @Override
    @SneakyThrows
    public void destroy() {
        if (registration != null) {
            registration.cancel();
        }
    }
}
