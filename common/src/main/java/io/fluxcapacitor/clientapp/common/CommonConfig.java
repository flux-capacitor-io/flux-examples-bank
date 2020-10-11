package io.fluxcapacitor.clientapp.common;

import io.fluxcapacitor.clientapp.common.elasticsearch.Elasticsearch;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
public class CommonConfig {

    @Bean
    public Elasticsearch elasticsearch() {
        return new Elasticsearch();
    }
}
