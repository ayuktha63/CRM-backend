package com.orque.crm.lead.dto;

import com.orque.crm.enums.LeadStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConvertContactToLeadRequest {

    private String assignedOwner;

    private LeadStatus status;

    private String notes;

    private BigDecimal estimatedValue;

    private LocalDate expectedCloseDate;
}