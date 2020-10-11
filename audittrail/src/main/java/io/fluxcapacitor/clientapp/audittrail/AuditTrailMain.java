package io.fluxcapacitor.clientapp.audittrail;

import io.fluxcapacitor.clientapp.common.elasticsearch.Elasticsearch;
import io.fluxcapacitor.clientapp.common.elasticsearch.Elasticsearch.Operation;
import io.fluxcapacitor.clientapp.common.elasticsearch.Elasticsearch.Operation.Action;
import io.fluxcapacitor.clientapp.common.elasticsearch.Range;
import io.fluxcapacitor.common.MessageType;
import io.fluxcapacitor.common.Registration;
import io.fluxcapacitor.common.api.SerializedMessage;
import io.fluxcapacitor.javaclient.FluxCapacitor;
import io.fluxcapacitor.javaclient.common.serialization.Serializer;
import io.fluxcapacitor.javaclient.tracking.ConsumerConfiguration;
import io.fluxcapacitor.javaclient.tracking.ErrorHandler;
import io.fluxcapacitor.javaclient.tracking.TrackingConfiguration;
import io.fluxcapacitor.javaclient.tracking.client.TrackingClient;
import io.fluxcapacitor.javaclient.tracking.client.TrackingUtils;
import io.fluxcapacitor.javaclient.tracking.metrics.CompleteMessageEvent;
import io.fluxcapacitor.javaclient.tracking.metrics.HandleMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static io.fluxcapacitor.common.MessageType.COMMAND;
import static io.fluxcapacitor.common.MessageType.ERROR;
import static io.fluxcapacitor.common.MessageType.EVENT;
import static io.fluxcapacitor.common.MessageType.METRICS;
import static io.fluxcapacitor.common.MessageType.QUERY;
import static io.fluxcapacitor.common.MessageType.RESULT;
import static io.fluxcapacitor.common.MessageType.SCHEDULE;
import static io.fluxcapacitor.javaclient.tracking.RetryingErrorHandler.forAnyError;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuditTrailMain implements InitializingBean, DisposableBean {

    private final Elasticsearch elasticsearch;
    private final FluxCapacitor fluxCapacitor;
    private final Serializer serializer;
    private Registration registration;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void afterPropertiesSet() {
        this.registration = Stream.concat(
                Stream.concat(Stream.of(COMMAND, EVENT, ERROR, SCHEDULE).map(t -> startTracking(t, "audittrail")),
                              Stream.of(QUERY, RESULT).map(t -> startTracking(t, "query-audittrail"))),
                Stream.of(METRICS).map(t -> startTracking(t, "metrics-audittrail")))
                .reduce(Registration::merge).orElse(Registration.noOp());
    }

    private Registration startTracking(MessageType messageType, String esIndex) {
        TrackingConfiguration trackingConfiguration = TrackingConfiguration.builder().ignoreMessageTarget(true)
                .maxFetchBatchSize(32).build();
        ConsumerConfiguration configuration = ConsumerConfiguration.getDefault(messageType).toBuilder()
                .trackingConfiguration(trackingConfiguration)
                .errorHandler(forAnyError())
                .build();
        String trackerName = format("%s:%s", fluxCapacitor.client().name(), configuration.getName());
        TrackingClient trackingClient = fluxCapacitor.client().getTrackingClient(messageType);
        AtomicReference<Instant> lastCleaningTime = new AtomicReference<>(Instant.now());
        log.info("Starting tracker {}", trackerName);
        Consumer<List<SerializedMessage>> consumer = messages -> {
            try {
                handle(messages, messageType, esIndex, lastCleaningTime);
            } catch (Exception e) {
                handleError(configuration.getErrorHandler(), trackerName,
                            () -> handle(messages, messageType, esIndex, lastCleaningTime), e);
            }
        };
        return TrackingUtils.start(trackerName, consumer, trackingClient,
                                   configuration.getTrackingConfiguration());
    }

    @SneakyThrows
    private void handleError(ErrorHandler errorHandler, String trackerName, Runnable handle, Exception e) {
        errorHandler.handleError(e, format("Handler %s failed to handle messages", trackerName), handle);
    }

    @SneakyThrows
    private void handle(List<SerializedMessage> messages, MessageType messageType, String esIndex,
                        AtomicReference<Instant> lastCleaningTime) {
        if (!messages.isEmpty()) {
            var operations = new ArrayList<Operation>();
            messages.forEach(m -> operations.add(new Operation(Action.index, esIndex, messageType.name() + m.getIndex(),
                                                               createEntry(messageType, m))));
            var errors = elasticsearch.bulk(operations).stream().filter(Objects::nonNull).collect(toList());
            if (!errors.isEmpty()) {
                log.error("Failed to insert {} batch ({}/{} errored, first message: {})", messageType,
                          errors.size(), messages.size(), describe(messages.get(0)), errors.get(0));
                throw errors.get(0);
            }
        }

        lastCleaningTime.getAndUpdate(last -> {
            if (Duration.ofMinutes(10).minus(Duration.between(last, Instant.now())).isNegative()) {
                purgeMessages(messageType, esIndex);
                return Instant.now();
            }
            return last;
        });
    }

    private String describe(SerializedMessage message) {
        return String.format("Index= %s, metadata= %s", message.getIndex(), message.getMetadata());
    }

    private Map<String, Object> createEntry(MessageType messageType, SerializedMessage m) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("messageType", messageType.name());
        value.put("messageId", m.getMessageId());
        value.put("type", m.getData().getType());
        value.put("revision", m.getData().getRevision());
        value.put("payload", new String(m.getData().getValue()));
        value.put("metadata", m.getMetadata());
        value.put("segment", m.getSegment());
        value.put("messageIndex", m.getIndex().toString());
        value.put("source", m.getSource());
        value.put("target", m.getTarget());
        value.put("requestId", m.getRequestId());
        value.put("@timestamp",
                  Instant.ofEpochMilli(messageType == SCHEDULE ? m.getIndex() >> 16 : m.getTimestamp()).toString());

        if (METRICS.equals(messageType)) {
            switch (m.getData().getType()) {
                case "io.fluxcapacitor.javaclient.tracking.metrics.HandleMessageEvent" -> {
                    HandleMessageEvent event = serializer.deserialize(m.getData());
                    value.put("handler", event.getHandler());
                    value.put("payloadType", event.getPayloadType());
                    value.put("millisecondDuration", Math.round((double) event.getNanosecondDuration() / 1_000_000));
                }
                case "io.fluxcapacitor.javaclient.tracking.metrics.CompleteMessageEvent" -> {
                    CompleteMessageEvent event = serializer.deserialize(m.getData());
                    value.put("handler", event.getHandler());
                    value.put("payloadType", event.getPayloadType());
                    value.put("millisecondDuration", Math.round((double) event.getNanosecondDuration() / 1_000_000));
                }
            }
        }

        return value;
    }

    private void purgeMessages(MessageType messageType, String esIndex) {
        executorService.submit(() -> {
            try {
                elasticsearch.deleteByQuery(esIndex, Map.of(
                        "messageType", messageType,
                        "@timestamp", Range.builder().lte(Instant.now().minus(getRetentionPeriod(messageType))).build()));
            } catch (Exception e) {
                log.error("Elasticsearch purge failed", e);
            }
        });
    }

    private Duration getRetentionPeriod(MessageType messageType) {
        return switch (messageType) {
            case EVENT -> Duration.ofDays(365);
            case METRICS -> Duration.ofDays(7);
            default -> Duration.ofDays(30);
        };
    }

    @Override
    @SneakyThrows
    public void destroy() {
        if (registration != null) {
            registration.cancel();
        }
    }
}
