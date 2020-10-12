package io.fluxcapacitor.clientapp.web.config;

import com.fasterxml.jackson.databind.JsonMappingException;
import io.fluxcapacitor.javaclient.common.exception.FunctionalException;
import io.fluxcapacitor.javaclient.common.exception.TechnicalException;
import io.fluxcapacitor.javaclient.common.serialization.SerializationException;
import io.fluxcapacitor.javaclient.tracking.handling.authentication.UnauthenticatedException;
import io.fluxcapacitor.javaclient.tracking.handling.authentication.UnauthorizedException;
import io.fluxcapacitor.javaclient.tracking.handling.validation.ValidationException;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.concurrent.CompletionException;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static javax.ws.rs.core.Response.status;

@Provider
@Slf4j
public class ErrorMapper implements ExceptionMapper<Exception> {

    @Context private HttpServletRequest request;

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof CompletionException && exception.getCause() instanceof Exception) {
            exception = (Exception) exception.getCause();
        }
        ErrorMessage errorMessage = new ErrorMessage(exception.getMessage());

        if (exception instanceof JsonMappingException) {
            log.warn("Failed to deserialize json for request {}", request.getRequestURI(), exception);
            return errorResponse(BAD_REQUEST, errorMessage);
        }
        if (exception instanceof ValidationException || exception instanceof SerializationException
                || exception instanceof NotFoundException) {
            return errorResponse(BAD_REQUEST, errorMessage);
        }
        if (exception instanceof UnauthenticatedException) {
            return status(UNAUTHORIZED)
                    .entity(errorMessage).type(MediaType.APPLICATION_JSON_TYPE).build();
        }
        if (exception instanceof UnauthorizedException) {
            return errorResponse(UNAUTHORIZED, errorMessage);
        }
        if (exception instanceof FunctionalException) {
            return errorResponse(FORBIDDEN, errorMessage);
        }
        if (exception instanceof WebApplicationException) {
            return errorResponse(((WebApplicationException) exception).getResponse().getStatusInfo().toEnum(),
                                 errorMessage);
        }
        if (exception instanceof TechnicalException) {
            return errorResponse(INTERNAL_SERVER_ERROR, errorMessage);
        }
        log.error("Error handling request {}", request.getRequestURI(), exception);
        return errorResponse(INTERNAL_SERVER_ERROR, errorMessage);
    }

    private Response errorResponse(Response.Status status, ErrorMessage errorMessage) {
        return status(status).entity(errorMessage).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    @Value
    private static class ErrorMessage {
        String error;
    }
}
