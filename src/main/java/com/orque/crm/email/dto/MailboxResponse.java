package com.orque.crm.email.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MailboxResponse {
    private Long id;
    private String emailAddress;
    private String displayName;
    private String provider;
    private String status;
    private LocalDateTime connectedAt;
}
