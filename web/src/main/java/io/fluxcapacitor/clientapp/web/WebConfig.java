package io.fluxcapacitor.clientapp.web;

import io.fluxcapacitor.clientapp.common.logging.LoggingInterceptor;
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
public class WebConfig {
    @Bean
    @ConditionalOnProperty("endpoint.messaging")
    public Client fluxCapacitorClient() {
        return WebSocketClient.newInstance(WebSocketClient.ClientConfig.builder().name("web")
                                                   .projectId("bank")
                                                   .serviceBaseUrl(getProperty("endpoint.messaging")).build());
    }

    @Autowired
    public void configure(FluxCapacitorBuilder builder) {
        builder.addDispatchInterceptor(LoggingInterceptor.instance).addHandlerInterceptor(LoggingInterceptor.instance);
    }
}

