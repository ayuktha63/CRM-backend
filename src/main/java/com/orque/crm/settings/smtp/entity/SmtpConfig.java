package com.orque.crm.settings.smtp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "smtp_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmtpConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    private Integer port;

    @Column(nullable = false)
    private String username;

    /** Stored as-is; encrypt at the service layer in production. */
    private String password;

    private String fromAddress;
    private String fromName;

    @Builder.Default
    private Boolean sslEnabled = false;

    @Builder.Default
    private Boolean tlsEnabled = true;

    /** True = this config is used when no explicit config is selected. */
    @Builder.Default
    private Boolean isDefault = false;

    /** Set to true after a successful test-connection call. */
    @Builder.Default
    private Boolean verified = false;

    private LocalDateTime lastTestedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
