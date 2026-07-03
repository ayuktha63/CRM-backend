package com.orque.crm.settings.smtp.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SmtpConfigResponse {
    private Long id;
    private String displayName;
    private String host;
    private Integer port;
    private String username;
    private String fromAddress;
    private String fromName;
    private Boolean sslEnabled;
    private Boolean tlsEnabled;
    private Boolean isDefault;
    private Boolean verified;
    private LocalDateTime lastTestedAt;
    private LocalDateTime createdAt;
}
