package com.orque.crm.email.entity;

import com.orque.crm.enums.EmailProvider;
import com.orque.crm.enums.MailboxStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One row per CRM user who has connected their own Gmail account for email sending.
 * Isolation: scoped by {organizationId, owner} — each user in each tenant has an
 * independent Google account connection, never shared across users or tenants.
 */
@Entity
@Table(name = "connected_mailboxes", uniqueConstraints = @UniqueConstraint(columnNames = "owner"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectedMailbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Username of the user who owns this mailbox. */
    @Column(nullable = false, unique = true)
    private String owner;

    /** Tenant this connection belongs to — never allow cross-tenant reuse. */
    private String organizationId;

    /** Human-readable label shown in the From dropdown. */
    private String displayName;

    private String emailAddress;

    @Enumerated(EnumType.STRING)
    private EmailProvider provider;

    @Enumerated(EnumType.STRING)
    private MailboxStatus status;

    @Column(columnDefinition = "TEXT")
    private String accessToken;

    @Column(columnDefinition = "TEXT")
    private String refreshToken;

    private LocalDateTime tokenExpiresAt;

    private LocalDateTime connectedAt;

    private LocalDateTime updatedAt;
}