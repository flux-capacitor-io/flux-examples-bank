package com.example.flux.bank.web;

import io.fluxcapacitor.clientapp.common.authentication.Sender;
import io.fluxcapacitor.common.api.Metadata;
import io.fluxcapacitor.common.serialization.JsonUtils;
import io.fluxcapacitor.javaclient.FluxCapacitor;
import io.fluxcapacitor.javaclient.common.Message;
import io.fluxcapacitor.javaclient.common.serialization.DeserializingMessage;
import io.fluxcapacitor.javaclient.tracking.handling.validation.ValidationException;
import io.fluxcapacitor.javaclient.web.HandleGet;
import io.fluxcapacitor.javaclient.web.HandlePost;
import io.fluxcapacitor.javaclient.web.WebRequest;
import io.fluxcapacitor.javaclient.web.WebResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static io.fluxcapacitor.clientapp.common.authentication.AuthenticationUtils.logoutCookie;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

@Component
@Slf4j
public class UiEndpoint {

    @HandlePost("api/command")
    public CompletableFuture<?> handleCommand(DeserializingMessage request, Sender sender) {
        return handleRequest(createMessage(request, sender), FluxCapacitor::sendCommand);
    }

    @HandlePost("api/query")
    public CompletableFuture<?> handleQuery(DeserializingMessage request, Sender sender) {
        return handleRequest(createMessage(request, sender), FluxCapacitor::query);
    }

    @HandleGet("api/logout")
    WebResponse handle() {
        return WebResponse.builder().cookie(logoutCookie()).status(302).header("Location", "/").build();
    }

    @HandleGet("api/health")
    public String healthCheck() {
        return "healthy";
    }

    @SneakyThrows
    private Message createMessage(DeserializingMessage request, Sender sender) {
        Object payload = JsonUtils.fromJson(request.getSerializedObject().data().getValue(), Object.class);
        Metadata metadata = payload instanceof Message message ? message.getMetadata() : Metadata.empty();

        return new Message(payload instanceof Message message ? message.getPayload() : payload,
                           addHeadersToMetadata(metadata, request)).addUser(sender);
    }

    private Metadata addHeadersToMetadata(Metadata metadata, DeserializingMessage request) {
        Map<String, List<String>> headers = WebRequest.getHeaders(request.getMetadata());
        Function<String, String> extractor = name -> headers.getOrDefault(
                name, emptyList()).stream().findFirst().orElse(null);
        return metadata.with("ipAddress", extractor.apply("X-Forwarded-For"),
                             "userAgent", extractor.apply("User-Agent"));
    }

    private CompletableFuture<?> handleRequest(Message message, Function<Message, CompletableFuture<?>> dispatcher) {
        if (message.getPayload() == null) {
            throw new ValidationException("Cannot handle commands or queries without a payload", emptySet());
        }
        return dispatcher.apply(message);
    }

}
