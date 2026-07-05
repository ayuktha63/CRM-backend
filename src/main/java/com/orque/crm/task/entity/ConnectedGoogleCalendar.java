package com.orque.crm.task.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One row per CRM user who has connected their own Google account for calendar sync.
 * Isolation: scoped by {organizationId, owner} — each user in each tenant has an
 * independent Google account connection, never shared across users or tenants.
 */
@Entity
@Table(name = "connected_google_calendars", uniqueConstraints = @UniqueConstraint(columnNames = "owner"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectedGoogleCalendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Username of the CRM user who owns this connection. */
    @Column(nullable = false, unique = true)
    private String owner;

    /** Tenant this connection belongs to — never allow cross-tenant reuse. */
    private String organizationId;

    /** The connected Google account's email address (fetched from Google userinfo). */
    private String googleEmail;

    @Column(columnDefinition = "TEXT")
    private String accessToken;

    @Column(columnDefinition = "TEXT")
    private String refreshToken;

    private LocalDateTime tokenExpiresAt;

    @Builder.Default
    private Boolean syncEnabled = true;

    private LocalDateTime connectedAt;

    private LocalDateTime updatedAt;

    private LocalDateTime lastSyncedAt;
}
