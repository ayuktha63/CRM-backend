package com.orque.crm.google.gmail.dto;

import lombok.Data;

import java.util.List;

@Data
public class SendEmailRequest {
    private List<String> to;
    private List<String> cc;
    private List<String> bcc;
    private String subject;
    private String bodyHtml;
}
