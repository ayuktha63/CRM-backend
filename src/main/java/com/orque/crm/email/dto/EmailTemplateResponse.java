package com.orque.crm.email.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EmailTemplateResponse {

    private Long id;

    private String templateName;

    private String subject;

    private String body;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}