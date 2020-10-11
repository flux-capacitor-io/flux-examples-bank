package io.fluxcapacitor.clientapp.common;

import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ClientHttpEngineBuilder43;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.util.function.UnaryOperator;

import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.client.ClientBuilder.newBuilder;

/**
 * Note that we use this only to set the correct cookie spec on the underlying Apache client.
 * From v5 of the client the "standard" cookie spec should be the default but until then this is the only viable way
 * to prevent set-cookie warnings in the log.
 *
 * See https://stackoverflow.com/questions/36473478/fixing-httpclient-warning-invalid-expires-attribute-using-fluent-api
 * or https://github.com/elastic/support-diagnostics/issues/233
 */
public class HttpUtils {

    public static Client httpClient(UnaryOperator<ClientBuilder> configurator) {
        ResteasyClientBuilder builder = (ResteasyClientBuilder) configurator.apply(
                        newBuilder().connectTimeout(10, SECONDS).readTimeout(45, SECONDS));
        ClientHttpEngine httpEngine = new ClientHttpEngineBuilder43().resteasyClientBuilder(builder).build();
        ReflectionUtils.setField("httpClient.defaultConfig.cookieSpec", httpEngine, "standard");
        return builder.httpEngine(httpEngine).build();
    }

    public static Client httpClient() {
        return httpClient(UnaryOperator.identity());
    }

    public static String readEntity(Response response) {
        try {
            return response.getEntity() == null ? null : response.readEntity(String.class);
        } catch (Exception ignored) {
            return response.getEntity().toString();
        }
    }
}
