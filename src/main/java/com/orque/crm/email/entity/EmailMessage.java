package com.orque.crm.email.entity;

import com.orque.crm.enums.EmailDirection;
import com.orque.crm.enums.EmailMessageStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long mailboxId;

    private Long contactId;

    private Long leadId;

    private String fromEmail;

    private String toEmail;

    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    private String gmailMessageId;

    private String gmailThreadId;

    @Enumerated(EnumType.STRING)
    private EmailDirection direction;

    @Enumerated(EnumType.STRING)
    private EmailMessageStatus status;

    private LocalDateTime sentAt;

    private LocalDateTime receivedAt;

    private LocalDateTime createdAt;

    // Email Workspace properties
    private String folder; // INBOX, SENT, DRAFT, TRASH, SPAM, ARCHIVE
    private Boolean isStarred;
    private Boolean isPinned;
    private Integer openCount;
    private Integer clickCount;
    private String bounceReason;
}