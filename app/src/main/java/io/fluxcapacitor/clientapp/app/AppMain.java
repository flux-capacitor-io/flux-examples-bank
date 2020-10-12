package io.fluxcapacitor.clientapp.app;

import lombok.extern.slf4j.Slf4j;

import static io.fluxcapacitor.clientapp.common.ApplicationUtils.startSpringApplication;

@Slf4j
public class AppMain {
    public static void main(String args[]) {
        startSpringApplication();
        log.info("App is running");
    }
}
