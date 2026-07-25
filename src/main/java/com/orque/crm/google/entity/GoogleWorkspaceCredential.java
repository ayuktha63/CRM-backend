package com.orque.crm.google.entity;

import com.orque.crm.google.crypto.GoogleTokenConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One row per CRM user who has connected their Google account. A single OAuth grant covers
 * Gmail, Calendar (and Meet, which rides on Calendar), and Tasks — there is no per-service
 * connection. Isolation: unique on {@code owner}, so each CRM user has exactly one independent
 * Google Workspace connection, never shared across users or tenants.
 */
@Entity
@Table(name = "google_workspace_credentials", uniqueConstraints = @UniqueConstraint(columnNames = "owner"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleWorkspaceCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Username of the CRM user who owns this connection. */
    @Column(nullable = false, unique = true)
    private String owner;

    /** Tenant this connection belongs to — never allow cross-tenant reuse. */
    private String organizationId;

    /** Google's stable account identifier (sub claim). */
    private String googleUserId;

    /** The connected Google account's email address. */
    private String email;

    @Convert(converter = GoogleTokenConverter.class)
    @Column(columnDefinition = "TEXT")
    private String accessToken;

    @Convert(converter = GoogleTokenConverter.class)
    @Column(columnDefinition = "TEXT")
    private String refreshToken;

    private LocalDateTime tokenExpiresAt;

    /** Space-separated scopes Google actually granted (may be a subset of what was requested). */
    @Column(columnDefinition = "TEXT")
    private String grantedScopes;

    private String tokenType;

    @Builder.Default
    private Boolean connected = true;

    /** Set when Google reports the refresh token is no longer valid (revoked by user or expired). */
    @Builder.Default
    private Boolean revoked = false;

    private LocalDateTime connectedAt;

    private LocalDateTime updatedAt;

    private LocalDateTime lastTokenRefreshAt;

    /** Last time any Google API call for this user succeeded — surfaced in the Integrations UI. */
    private LocalDateTime lastApiSuccessAt;

    public boolean hasScope(String scope) {
        return grantedScopes != null && grantedScopes.contains(scope);
    }
}
