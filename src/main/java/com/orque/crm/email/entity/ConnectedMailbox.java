package com.orque.crm.email.entity;

import com.orque.crm.enums.EmailProvider;
import com.orque.crm.enums.MailboxStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "connected_mailboxes")
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
    private String owner;

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