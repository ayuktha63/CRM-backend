package com.orque.crm.email.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GmailInboxMessageResponse {

    private String gmailMessageId;

    private String gmailThreadId;

    private String fromEmail;

    private String toEmail;

    private String subject;

    private String snippet;
}