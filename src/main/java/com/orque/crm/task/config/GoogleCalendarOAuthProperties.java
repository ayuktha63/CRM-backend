package com.orque.crm.task.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "google.calendar.oauth")
public class GoogleCalendarOAuthProperties {

    private String clientId;

    private String clientSecret;

    private String redirectUri;

    private String scope;
}
