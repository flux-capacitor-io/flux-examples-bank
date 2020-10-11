package io.fluxcapacitor.clientapp.common.admin;

import io.fluxcapacitor.clientapp.common.PropertyUtils;
import io.fluxcapacitor.clientapp.common.admin.command.AppendMessages;
import io.fluxcapacitor.clientapp.common.admin.command.DeleteAggregate;
import io.fluxcapacitor.clientapp.common.admin.command.DeleteValue;
import io.fluxcapacitor.clientapp.common.admin.command.DisconnectTracker;
import io.fluxcapacitor.clientapp.common.admin.command.EncryptValue;
import io.fluxcapacitor.clientapp.common.admin.command.ResetTrackingPosition;
import io.fluxcapacitor.clientapp.common.admin.command.RetryCommand;
import io.fluxcapacitor.clientapp.common.admin.command.RetryCommands;
import io.fluxcapacitor.clientapp.common.admin.command.RetryEvent;
import io.fluxcapacitor.clientapp.common.admin.command.RetryEvents;
import io.fluxcapacitor.clientapp.common.admin.command.ScheduleMessage;
import io.fluxcapacitor.clientapp.common.admin.command.StoreDomainEvent;
import io.fluxcapacitor.clientapp.common.admin.command.StoreValue;
import io.fluxcapacitor.clientapp.common.admin.query.GetAggregate;
import io.fluxcapacitor.clientapp.common.admin.query.GetDeserializedEvents;
import io.fluxcapacitor.clientapp.common.admin.query.GetEventBatch;
import io.fluxcapacitor.clientapp.common.admin.query.GetMessagesFromIndex;
import io.fluxcapacitor.clientapp.common.admin.query.GetStoredValue;
import io.fluxcapacitor.common.api.Metadata;
import io.fluxcapacitor.common.api.SerializedMessage;
import io.fluxcapacitor.common.api.eventsourcing.EventBatch;
import io.fluxcapacitor.common.api.eventsourcing.GetEvents;
import io.fluxcapacitor.common.api.eventsourcing.GetEventsResult;
import io.fluxcapacitor.javaclient.FluxCapacitor;
import io.fluxcapacitor.javaclient.common.Message;
import io.fluxcapacitor.javaclient.common.serialization.DeserializingMessage;
import io.fluxcapacitor.javaclient.common.websocket.AbstractWebsocketClient;
import io.fluxcapacitor.javaclient.persisting.eventsourcing.client.WebSocketEventStoreClient;
import io.fluxcapacitor.javaclient.scheduling.Schedule;
import io.fluxcapacitor.javaclient.tracking.handling.HandleCommand;
import io.fluxcapacitor.javaclient.tracking.handling.HandleQuery;
import io.fluxcapacitor.javaclient.tracking.handling.LocalHandler;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static io.fluxcapacitor.common.MessageType.COMMAND;
import static io.fluxcapacitor.common.MessageType.EVENT;
import static io.fluxcapacitor.javaclient.FluxCapacitor.loadAggregate;
import static io.fluxcapacitor.javaclient.FluxCapacitor.publishEvent;
import static io.fluxcapacitor.javaclient.FluxCapacitor.sendAndForgetCommand;
import static java.util.stream.Collectors.toList;

@Component
@AllArgsConstructor
@LocalHandler
@Slf4j
public class AdminHandler {

    /*
        EVENT STORE STUFF
     */

    @HandleQuery
    @SneakyThrows
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public EventBatch handle(GetEventBatch query) {
        WebSocketEventStoreClient eventStoreClient
                = (WebSocketEventStoreClient) FluxCapacitor.get().client().getEventStoreClient();
        Method sendRequestAndWait = Arrays.stream(AbstractWebsocketClient.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("sendRequestAndWait")).findFirst().get();
        sendRequestAndWait.setAccessible(true);
        GetEventsResult result = (GetEventsResult) sendRequestAndWait.invoke(
                eventStoreClient, new GetEvents(query.getAggregateId(),
                                                query.getLastSequenceNumber() == null ? -1L :
                                                        query.getLastSequenceNumber(),
                                                query.getBatchSize() == null ? 1024 : query.getBatchSize()));
        return result.getEventBatch();
    }

    @HandleQuery
    @SneakyThrows
    public Object handle(GetAggregate query) {
        return loadAggregate(query.getAggregateId(), Class.forName(query.getClassName())).get();
    }

    @HandleQuery
    public List<Message> handle(GetDeserializedEvents query) {
        return FluxCapacitor.get().eventStore().getDomainEvents(query.getAggregateId())
                .map(DeserializingMessage::toMessage).collect(toList());
    }

    @HandleCommand
    public void handle(StoreDomainEvent command) {
        FluxCapacitor.get().eventStore().storeDomainEvents(
                command.getAggregateId(), command.getDomain(), command.getSequenceNumber(),
                new Message(command.getEvent(), command.getMetadata()));
    }

    @HandleCommand
    public CompletableFuture<Boolean> handle(DeleteAggregate command) {
        return FluxCapacitor.get().client().getEventStoreClient().deleteEvents(command.getAggregateId());
    }

    /*
        TRACKING STUFF
     */

    @HandleCommand
    public void handle(ScheduleMessage command) {
        FluxCapacitor.get().scheduler()
                .schedule(new Schedule(command.getMessage(), command.getScheduleId(), command.getDeadline()));
    }

    @HandleCommand
    @SneakyThrows
    public void handle(ResetTrackingPosition command) {
        FluxCapacitor.get().client().getTrackingClient(command.getMessageType())
                .resetPosition(command.getConsumer(), command.getLastIndex()).await();
    }

    @HandleQuery
    public List<SerializedMessage> handle(GetMessagesFromIndex query) {
        return FluxCapacitor.get().client().getTrackingClient(query.getMessageType())
                .readFromIndex(query.getMinIndex(), query.getMaxSize());
    }

    @HandleCommand
    public void handle(DisconnectTracker command) {
        FluxCapacitor.get().client().getTrackingClient(command.getMessageType())
                .disconnectTracker(command.getConsumer(), command.getTrackingId(), command.getSendFinalEmptyBatch());
    }

    @HandleCommand
    @SneakyThrows
    public void handle(RetryCommand command) {
        Optional<SerializedMessage> serializedCommand = FluxCapacitor.get().client().getTrackingClient(COMMAND)
                .readFromIndex(command.getIndex(), 1)
                .stream().filter(c -> c.getIndex().equals(command.getIndex())).findFirst();
        if (serializedCommand.isEmpty()) {
            throw new IllegalArgumentException("Could not find command with index " + command.getIndex());
        }
        SerializedMessage message = serializedCommand.get();
        message.getMetadata().put("retriedIndex", Long.valueOf(command.getIndex()).toString());
        message.setIndex(null);
        message.setRequestId(null);
        message.setSource(null);
        message.setTimestamp(System.currentTimeMillis());

        FluxCapacitor.get().client().getGatewayClient(COMMAND).send(message).await();
        log.info("Retried command {} with index {}", message.getData().getType(), message.getIndex());
    }

    @HandleCommand
    @SneakyThrows
    public void handle(RetryEvent command) {
        Optional<SerializedMessage> serializedEvent = FluxCapacitor.get().client().getTrackingClient(EVENT)
                .readFromIndex(command.getIndex(), 1)
                .stream().filter(c -> c.getIndex().equals(command.getIndex())).findFirst();
        if (serializedEvent.isEmpty()) {
            throw new IllegalArgumentException("Could not find event with index " + command.getIndex());
        }
        SerializedMessage message = serializedEvent.get();
        message.getMetadata().put("retriedIndex", Long.valueOf(command.getIndex()).toString());
        message.setIndex(null);
        message.setRequestId(null);
        message.setSource(null);
        message.setTimestamp(System.currentTimeMillis());

        FluxCapacitor.get().client().getGatewayClient(EVENT).send(message).await();
        log.info("Retried event {} with index {}", message.getData().getType(), message.getIndex());
    }

    @HandleCommand
    public void handle(AppendMessages command, Metadata metadata) {
        switch (command.getType()) {
            case EVENT:
                command.getMessages().forEach(m -> {
                    if (m instanceof Message) {
                        publishEvent(m);
                    } else {
                        publishEvent(m, metadata);
                    }
                });
                break;
            case COMMAND:
                command.getMessages().forEach(m -> {
                    if (m instanceof Message) {
                        sendAndForgetCommand(m);
                    } else {
                        sendAndForgetCommand(m, metadata);
                    }
                });
                break;
            default:
                throw new UnsupportedOperationException("Unsupported message type:" + command.getType());
        }
    }

    @HandleCommand
    public void handle(RetryCommands command) {
        command.getIndexes().forEach(c -> handle(new RetryCommand(Long.parseLong(c))));
    }

    @HandleCommand
    public void handle(RetryEvents event) {
        event.getIndexes().forEach(c -> handle(new RetryEvent(Long.parseLong(c))));
    }

    /*
        KEY VALUE STORE STUFF
     */

    @HandleQuery
    public Object handle(GetStoredValue query) {
        return FluxCapacitor.get().keyValueStore().get(query.getKey());
    }

    @HandleCommand
    public void handle(DeleteValue command) {
        FluxCapacitor.get().keyValueStore().delete(command.getKey());
    }

    @HandleCommand
    public void handle(StoreValue command) {
        FluxCapacitor.get().keyValueStore().store(command.getKey(), command.getValue());
    }

    /*
        OTHER STUFF
     */

    @HandleCommand
    @SneakyThrows
    public String handle(EncryptValue command) {
        return PropertyUtils.encryptProperty(command.getValue());
    }

}
