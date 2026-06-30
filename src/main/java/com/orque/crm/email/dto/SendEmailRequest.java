package com.orque.crm.email.dto;

import lombok.Data;

@Data
public class SendEmailRequest {

    private Long mailboxId;

    private Long contactId;

    private Long leadId;

    private String toEmail;

    private String cc;

    private String bcc;

    private String subject;

    private String body;

    private String scheduledAt;

    private Boolean isDraft;
}