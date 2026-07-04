package com.orque.crm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Single shared RestTemplate bean reused across all outbound HTTP calls (OPAC
 * validation, Google OAuth, Gmail API) instead of allocating a new instance
 * (and its underlying connection factory) on every call.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
