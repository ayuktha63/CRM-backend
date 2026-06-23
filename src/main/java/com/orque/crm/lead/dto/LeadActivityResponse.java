package com.orque.crm.lead.dto;

import com.orque.crm.enums.LeadActivityType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadActivityResponse {

    private Long id;

    private Long leadId;

    private LeadActivityType activityType;

    private String description;

    private LocalDateTime createdAt;
}