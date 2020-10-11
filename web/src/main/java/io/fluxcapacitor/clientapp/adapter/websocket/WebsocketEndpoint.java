package io.fluxcapacitor.clientapp.adapter.websocket;

import io.fluxcapacitor.clientapp.adapter.subscription.SubscribeToUpdates;
import io.fluxcapacitor.clientapp.common.authentication.AuthenticationToken;
import io.fluxcapacitor.common.ConsistentHashing;
import io.fluxcapacitor.common.Registration;
import io.fluxcapacitor.javaclient.tracking.handling.authentication.User;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static io.fluxcapacitor.clientapp.adapter.authentication.AuthenticationUtils.createUser;
import static io.fluxcapacitor.javaclient.FluxCapacitor.queryAndWait;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

@ServerEndpoint(value = "/api/websocket", encoders = JsonEncoder.class)
@Slf4j
public class WebsocketEndpoint {
    private static final Map<String, Registration> connectedClients = new ConcurrentHashMap<>();
    private static final List<ScheduledExecutorService> sendExecutors =
            range(0, 32).mapToObj(i -> newSingleThreadScheduledExecutor()).collect(toList());

    @OnOpen
    public void onOpen(Session session) {
        authenticate(session).ifPresent(user -> {
            try {
                User.current.set(user);
                ScheduledExecutorService executor = getExecutor(session);
                Map<String, Object> userProperties = Collections.synchronizedMap(session.getUserProperties());
                Registration registration = queryAndWait(new SubscribeToUpdates(
                        update -> {
                            userProperties.put(update.getRoutingKey(), update.getValue());
                            executor.submit(() -> {
                                if (userProperties.remove(update.getRoutingKey(), update.getValue())) {
                                    sendToUser(session, user, () -> session.getBasicRemote().sendObject(update.getValue()));
                                }
                            });
                        }));
                connectedClients.put(session.getId(), registration);
                keepAlive(session, user);
            } finally {
                User.current.remove();
            }
        });
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        log.info("Websocket closed, session {} (close reason: {})", session.getId(), closeReason);
        Optional.ofNullable(connectedClients.remove(session.getId())).ifPresent(Registration::cancel);
    }

    @SneakyThrows
    private Optional<User> authenticate(Session session) {
        try {
            return Optional.of(createUser(Optional.ofNullable(
                    session.getRequestParameterMap().get("jwt")).flatMap(h -> h.stream().findFirst())
                                                  .map(t -> AuthenticationToken.builder().value(t).build())
                                                  .orElse(null)));
        } catch (Exception e) {
            session.close(new CloseReason(UnauthenticatedCloseCode.INSTANCE, "Unauthenticated"));
            return Optional.empty();
        }
    }

    enum UnauthenticatedCloseCode implements CloseReason.CloseCode {
        INSTANCE {
            @Override
            public int getCode() {
                return 4001;
            }
        };
    }

    private void keepAlive(Session session, User user) {
        getExecutor(session).schedule(() -> {
            if (connectedClients.containsKey(session.getId())) {
                try {
                    sendToUser(session, user,
                               () -> session.getBasicRemote().sendPing(ByteBuffer.wrap(new byte[]{(byte) 1})));
                } finally {
                    keepAlive(session, user);
                }
            }
        }, 30, TimeUnit.SECONDS);
    }

    private void sendToUser(Session session, User user, Executable action) {
        try {
            if (session.isOpen()) {
                action.run();
            }
        } catch (ClosedChannelException e) {
            log.info("Websocket of user {} closed while sending value. Session {}", user.getName(), session.getId());
        } catch (Exception e) {
            log.warn("Failed to send value to user {}, session {}", user.getName(), session.getId(), e);
        }
    }

    private ScheduledExecutorService getExecutor(Session session) {
        int segment = ConsistentHashing.computeSegment(session.getId(), sendExecutors.size());
        return sendExecutors.get(segment);
    }

    @FunctionalInterface
    private interface Executable {
        void run() throws Exception;
    }
}
