package com.fecredit.gbg.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import lombok.Data;

@Configuration
public class MicrosoftGraphConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
