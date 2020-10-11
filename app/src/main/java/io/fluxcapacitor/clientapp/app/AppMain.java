package io.fluxcapacitor.clientapp.app;

import io.fluxcapacitor.clientapp.adapter.AdapterMain;
import lombok.extern.slf4j.Slf4j;

import static io.fluxcapacitor.clientapp.common.ApplicationUtils.startSpringApplication;
import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;

@Slf4j
public class AppMain {
    public static void main(String args[]) {
        startSpringApplication();
        int port = parseInt(getProperty("port", "8080"));
        AdapterMain.startServer(port);
        log.info("App is running on port {}", port);
    }
}
