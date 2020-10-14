package io.fluxcapacitor.clientapp.web;

import io.fluxcapacitor.clientapp.common.ApplicationUtils;
import io.fluxcapacitor.clientapp.web.authentication.AuthenticationFilter;
import io.fluxcapacitor.clientapp.web.config.ErrorMapper;
import io.fluxcapacitor.clientapp.web.config.JacksonConfig;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Set;
import java.util.stream.Stream;

import static io.fluxcapacitor.clientapp.common.ApplicationUtils.startSpringApplication;
import static java.lang.System.getProperty;
import static java.util.stream.Collectors.toSet;

@ApplicationPath("/api")
@Slf4j
public class WebMain extends Application {
    private final Set<Object> singletons;

    public WebMain() {
        singletons = Stream.of(new JacksonConfig(), new ErrorMapper(), new HealthEndpoint(), new AuthenticationFilter(),
                               new MainEndpoint()).collect(toSet());
        if (ApplicationUtils.isDevMode()) {
            log.info("Starting adapter in dev mode");
        }
    }

    public static void main(String args[]) {
        startSpringApplication();
        startServer(Integer.parseInt(getProperty("port", "8080")));
    }

    @SneakyThrows
    public static Undertow startServer(int port) {
        PathHandler path = Handlers.path();
        DeploymentInfo deployment = Servlets.deployment().setClassLoader(WebMain.class.getClassLoader())
                .setContextPath("/")
                .addServlet(Servlets.servlet(HttpServlet30Dispatcher.class).setAsyncSupported(true)
                                    .setLoadOnStartup(1).addMapping("/api/*")
                                    .addInitParam(ResteasyContextParameters.RESTEASY_SERVLET_MAPPING_PREFIX, "/api")
                                    .addInitParam("javax.ws.rs.Application", WebMain.class.getName()))
                .setDeploymentName("adapter");

        ServletContainer container = Servlets.defaultContainer();
        DeploymentManager manager = container.addDeployment(deployment);
        manager.deploy();
        path.addPrefixPath(deployment.getContextPath(), manager.start());

        Undertow server = Undertow.builder()
                .addHttpListener(port, "0.0.0.0")
                .setHandler(path)
                .build();
        server.start();
        log.info("Adapter is running on port {}", port);
        return server;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

}
