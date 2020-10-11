package io.fluxcapacitor.clientapp.adapter.subscription;

import io.fluxcapacitor.common.Registration;
import io.fluxcapacitor.javaclient.tracking.handling.HandleQuery;
import io.fluxcapacitor.javaclient.tracking.handling.LocalHandler;
import io.fluxcapacitor.javaclient.tracking.handling.authentication.User;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

import static java.util.Collections.emptySet;

@Component
public class SubscriptionHandler {

    private final Map<User, Set<Consumer<Update>>> subscribers = new ConcurrentHashMap<>();

    void sendUpdate(String userId, Update update) {
        subscribers.entrySet().stream().filter(e -> Objects.equals(e.getKey().getName(), userId))
                .forEach(e -> e.getValue().forEach(user -> user.accept(update)));
    }

    @HandleQuery
    @LocalHandler
    public Registration handle(SubscribeToUpdates query) {
        User user = User.current.get();
        Set<Consumer<Update>> consumers = subscribers.computeIfAbsent(user, c -> new CopyOnWriteArraySet<>());
        Consumer<Update> consumer = query.getConsumerFunction();
        consumers.add(consumer);
        return () -> {
            consumers.remove(consumer);
            subscribers.remove(user, emptySet());
        };
    }

}
