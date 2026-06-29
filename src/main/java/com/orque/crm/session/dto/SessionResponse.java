package com.orque.crm.session.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SessionResponse {
    private Long id;
    private Long userId;
    private String username;
    private String role;
    private LocalDateTime loginTime;
    private LocalDateTime lastActivity;
    private LocalDateTime logoutTime;
    private String ipAddress;
    private String browser;
    private String operatingSystem;
    private String device;
    private String status;
    private Long durationMinutes;
}
