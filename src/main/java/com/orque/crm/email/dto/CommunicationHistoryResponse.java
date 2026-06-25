package com.orque.crm.email.dto;

import com.orque.crm.enums.EmailActivityType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CommunicationHistoryResponse {

    private Long id;

    private Long contactId;

    private Long leadId;

    private Long emailMessageId;

    private EmailActivityType activityType;

    private String description;

    private LocalDateTime activityAt;
}