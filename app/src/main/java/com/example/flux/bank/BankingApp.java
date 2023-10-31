package com.example.flux.bank;

import io.fluxcapacitor.javaclient.configuration.FluxCapacitorBuilder;
import io.fluxcapacitor.javaclient.configuration.client.Client;
import io.fluxcapacitor.javaclient.configuration.client.WebSocketClient;
import io.fluxcapacitor.javaclient.configuration.spring.ConditionalOnProperty;
import io.fluxcapacitor.javaclient.configuration.spring.FluxCapacitorSpringConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static io.fluxcapacitor.javaclient.configuration.ApplicationProperties.getProperty;

@Configuration
@ComponentScan
@Import(FluxCapacitorSpringConfig.class)
@Slf4j
public class BankingApp {
    public static void main(String... args) {
        new AnnotationConfigApplicationContext(BankingApp.class).registerShutdownHook();
        log.info("Banking application is running");
    }

    @Bean
    @ConditionalOnProperty("FLUX_URL")
    public Client fluxCapacitorClient() {
        return WebSocketClient.newInstance(
                WebSocketClient.ClientConfig.builder()
                        .name("banking-app")
                        .projectId(getProperty("PROJECT_ID"))
                        .serviceBaseUrl(getProperty("FLUX_URL"))
                        .build());
    }

    @Autowired
    public void configure(FluxCapacitorBuilder builder) {
        //configure Flux here
    }

}
