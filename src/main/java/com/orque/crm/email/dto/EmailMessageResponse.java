package com.orque.crm.email.dto;

import com.orque.crm.enums.EmailDirection;
import com.orque.crm.enums.EmailMessageStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EmailMessageResponse {

    private Long id;

    private Long contactId;

    private Long leadId;

    private String fromEmail;

    private String toEmail;

    private String subject;

    private String body;

    private String gmailMessageId;

    private String gmailThreadId;

    private EmailDirection direction;

    private EmailMessageStatus status;

    private LocalDateTime sentAt;

    private LocalDateTime receivedAt;

    private String folder;
    private Boolean isStarred;
    private Boolean isPinned;
    private Integer openCount;
    private Integer clickCount;
    private String bounceReason;

    private String cc;
    private String bcc;
    private LocalDateTime scheduledAt;
    private Boolean isDraft;
    private Boolean isRead;
}