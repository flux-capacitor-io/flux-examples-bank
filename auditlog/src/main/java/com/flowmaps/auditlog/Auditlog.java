package com.flowmaps.auditlog;

import com.flowmaps.api.application.common.Environment;
import io.fluxcapacitor.javaclient.configuration.ApplicationProperties;
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

@Configuration
@Import(FluxCapacitorSpringConfig.class)
@ComponentScan
@Slf4j
public class Auditlog {
    public static void main(String... args) {
        new AnnotationConfigApplicationContext(Auditlog.class).registerShutdownHook();
        log.info("Flowmaps auditlog is running (environment {})", Environment.getEnvironment());
    }

    @Bean
    @ConditionalOnProperty("FLUX_BASE_URL")
    public Client fluxCapacitorClient() {
        return WebSocketClient.newInstance(
                WebSocketClient.ClientConfig.builder()
                        .name("flowmaps-auditlog")
                        .projectId(ApplicationProperties.getProperty("PROJECT_ID"))
                        .serviceBaseUrl(ApplicationProperties.getProperty("FLUX_BASE_URL"))
                        .build());
    }

    @Bean
    public OpenSearch elasticsearch() {
        return new OpenSearch();
    }

    @Autowired
    public void configure(FluxCapacitorBuilder builder) {
        builder.disableErrorReporting()
                .disableAutomaticAggregateCaching()
                .disableScheduledCommandHandler()
                .disableTrackingMetrics();
    }
}
