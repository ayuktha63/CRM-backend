package com.orque.crm.settings.smtp.dto;

import lombok.Data;

@Data
public class SmtpConfigRequest {
    private String displayName;
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String fromAddress;
    private String fromName;
    private Boolean sslEnabled;
    private Boolean tlsEnabled;
    private Boolean isDefault;
}
