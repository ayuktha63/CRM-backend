package com.orque.crm.email.dto;

import lombok.Data;

@Data
public class EmailTemplateRequest {

    private String templateName;

    private String subject;

    private String body;
}