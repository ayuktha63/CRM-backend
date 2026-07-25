package com.orque.crm.google.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "google.workspace.oauth")
public class GoogleWorkspaceProperties {

    private String clientId;

    private String clientSecret;

    private String redirectUri;

    /** Space-separated scope list requested at consent time. */
    private String scope;
}
