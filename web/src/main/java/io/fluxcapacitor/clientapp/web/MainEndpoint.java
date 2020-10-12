package io.fluxcapacitor.clientapp.web;

import io.fluxcapacitor.clientapp.common.authentication.AppUser;
import io.fluxcapacitor.clientapp.web.authentication.AuthenticateClient;
import io.fluxcapacitor.common.api.Metadata;
import io.fluxcapacitor.javaclient.FluxCapacitor;
import io.fluxcapacitor.javaclient.common.Message;
import io.fluxcapacitor.javaclient.tracking.handling.authentication.User;
import io.fluxcapacitor.javaclient.tracking.handling.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import java.util.function.Function;

import static java.util.Collections.emptySet;
import static javax.ws.rs.core.HttpHeaders.USER_AGENT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("")
@AuthenticateClient
@Produces(APPLICATION_JSON)
@Slf4j
public class MainEndpoint {

    /*
        Endpoints for queries and commands from users
     */

    @POST
    @Path("/command")
    public Object handleCommand(Object payload, @Context HttpHeaders headers) {
        return handleRequest(createMessage(payload, headers), FluxCapacitor::sendCommandAndWait);
    }

    @POST
    @Path("/query")
    public Object handleQuery(Object payload, @Context HttpHeaders headers) {
        return handleRequest(createMessage(payload, headers), FluxCapacitor::queryAndWait);
    }

    /*
        Helper methods
     */

    private Object handleRequest(Message message, Function<Message, Object> dispatcher) {
        if (message.getPayload() == null) {
            log.warn("Failed to handle {}. Payload is missing.", message);
            throw new ValidationException("Cannot handle commands or queries without a payload", emptySet());
        }
        return dispatcher.apply(message);
    }

    private Message createMessage(Object payload, HttpHeaders headers) {
        AppUser user = User.getCurrent();
        if (payload instanceof Message) {
            Message message = (Message) payload;
            if (user.isAdmin() && message.getMetadata().containsKey(AppUser.metadataKey)) {
                return new Message(message.getPayload(), message.getMetadata());
            }
            return new Message(message.getPayload(),
                               user.addTo(addHeadersToMetadata(message.getMetadata(), headers)));
        } else {
            return new Message(payload, addHeadersToMetadata(user.asMetadata(), headers));
        }
    }

    private Metadata addHeadersToMetadata(Metadata metadata, HttpHeaders headers) {
        metadata.put("ipAddress", headers.getRequestHeader("X-Forwarded-For").stream().findFirst()
                .orElse("Unknown IP address"));
        metadata.put("userAgent", headers.getHeaderString(USER_AGENT));
        return metadata;
    }

}
