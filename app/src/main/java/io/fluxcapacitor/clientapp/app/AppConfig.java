package io.fluxcapacitor.clientapp.app;

import io.fluxcapacitor.javaclient.configuration.FluxCapacitorBuilder;
import io.fluxcapacitor.javaclient.configuration.client.Client;
import io.fluxcapacitor.javaclient.configuration.client.WebSocketClient;
import io.fluxcapacitor.javaclient.configuration.spring.ConditionalOnProperty;
import io.fluxcapacitor.javaclient.configuration.spring.FluxCapacitorSpringConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static java.lang.System.getProperty;


@Configuration
@Import(FluxCapacitorSpringConfig.class)
@ComponentScan
public class AppConfig {
    @Bean
    @ConditionalOnProperty("endpoint.messaging")
    public Client fluxCapacitorClient() {
        return WebSocketClient.newInstance(WebSocketClient.Properties.builder().name("app")
                                                   .projectId("bank")
                                                   .serviceBaseUrl(getProperty("endpoint.messaging")).build());
    }

    @Autowired
    public void configure(FluxCapacitorBuilder builder) {
        builder.enableTrackingMetrics();
    }
}

