package io.fluxcapacitor.clientapp.adapter;

import io.fluxcapacitor.clientapp.adapter.authentication.AuthenticationFilter;
import io.fluxcapacitor.clientapp.adapter.config.ErrorMapper;
import io.fluxcapacitor.clientapp.adapter.config.JacksonConfig;
import io.fluxcapacitor.clientapp.adapter.websocket.WebsocketEndpoint;
import io.fluxcapacitor.clientapp.common.Environment;
import io.fluxcapacitor.clientapp.common.PropertyUtils;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.connector.ByteBufferPool;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.GracefulShutdownHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;

import javax.servlet.DispatcherType;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Set;
import java.util.stream.Stream;

import static io.fluxcapacitor.clientapp.common.ApplicationUtils.startSpringApplication;
import static java.lang.Runtime.getRuntime;
import static java.lang.System.getProperty;
import static java.util.stream.Collectors.toSet;

@ApplicationPath("/api")
@Slf4j
public class AdapterMain extends Application {

    private static final ByteBufferPool bufferPool =
            new DefaultByteBufferPool(false, 1024, 100, 12);

    private final Set<Object> singletons;

    public AdapterMain() {
        singletons = Stream.of(new JacksonConfig(), new ErrorMapper(), new HealthEndpoint(), new AuthenticationFilter(), new AdapterEndpoint())
                .collect(toSet());

        if (PropertyUtils.isDevMode()) {
            log.info("Starting adapter in dev mode");

            if (PropertyUtils.getEnvironment() == Environment.test) {
                singletons.add(new IntegrationTestRequestFilter());
            }
        }
    }

    //use to start independent adapter – not required when running everything from app
    public static void main(String args[]) {
        startSpringApplication();
        startServer(Integer.parseInt(getProperty("port", "8080")));
    }

    @SneakyThrows
    public static Undertow startServer(int port) {
        PathHandler path = Handlers.path();
        DeploymentInfo deployment = Servlets.deployment().setClassLoader(AdapterMain.class.getClassLoader())
                .setContextPath("/")
                //JAX-RS (Resteasy)
                .addServlet(Servlets.servlet(HttpServlet30Dispatcher.class).setAsyncSupported(true)
                        .setLoadOnStartup(1).addMapping("/api/*")
                        .addInitParam(ResteasyContextParameters.RESTEASY_SERVLET_MAPPING_PREFIX, "/api")
                        .addInitParam("javax.ws.rs.Application", AdapterMain.class.getName()))
                //WebSocket
                .addFilter(new FilterInfo("websocketFilter", AuthenticationFilter.class))
                .addFilterUrlMapping("websocketFilter", "/api/websocket", DispatcherType.REQUEST)
                .addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME,
                        new WebSocketDeploymentInfo().setBuffers(bufferPool)
                                .addEndpoint(WebsocketEndpoint.class))
                .setDeploymentName("adapter");

        ServletContainer container = Servlets.defaultContainer();
        DeploymentManager manager = container.addDeployment(deployment);
        manager.deploy();
        path.addPrefixPath(deployment.getContextPath(), manager.start());

        //Request timeout needs to be greater then ELB request timeout (60 sec)
        Undertow server = Undertow.builder()
                .addHttpListener(port, "0.0.0.0")
                .setHandler(addShutdownHandler(new EncodingHandler.Builder().build(null).wrap(path)))
                .setServerOption(io.undertow.UndertowOptions.NO_REQUEST_TIMEOUT, 75 * 1000)
                .build();

        server.start();
        log.info("Adapter is running on port {}", port);
        return server;
    }

    private static HttpHandler addShutdownHandler(HttpHandler httpHandler) {
        GracefulShutdownHandler shutdownHandler = new GracefulShutdownHandler(httpHandler);
        getRuntime().addShutdownHook(new Thread(() -> {
            shutdownHandler.shutdown();
            try {
                shutdownHandler.awaitShutdown(1000);
            } catch (InterruptedException e) {
                log.warn("Thread to kill server was interrupted");
                Thread.currentThread().interrupt();
            }
        }));
        return shutdownHandler;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

}
