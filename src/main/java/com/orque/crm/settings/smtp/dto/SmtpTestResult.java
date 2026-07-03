package com.orque.crm.settings.smtp.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SmtpTestResult {
    private boolean success;
    private String message;
}
