package com.orque.crm.session.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private String username;
    private String role;

    @Column(length = 512)
    private String jwtId;        // JWT subject or a hash of the token

    private LocalDateTime loginTime;
    private LocalDateTime lastActivity;
    private LocalDateTime logoutTime;

    private String ipAddress;
    private String browser;
    private String operatingSystem;
    private String device;

    @Column(length = 20)
    private String status;       // ACTIVE | LOGGED_OUT | EXPIRED | TERMINATED

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (loginTime == null) loginTime = LocalDateTime.now();
        if (lastActivity == null) lastActivity = LocalDateTime.now();
        if (status == null) status = "ACTIVE";
    }
}
