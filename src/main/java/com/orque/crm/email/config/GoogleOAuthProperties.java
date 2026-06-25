package com.orque.crm.email.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "google.oauth")
public class GoogleOAuthProperties {

    private String clientId;

    private String clientSecret;

    private String redirectUri;

    private String scope;
}