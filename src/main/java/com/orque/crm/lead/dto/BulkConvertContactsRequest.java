package com.orque.crm.lead.dto;

import com.orque.crm.enums.LeadStatus;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkConvertContactsRequest {

    @NotEmpty(message = "Contact IDs are required")
    private List<Long> contactIds;

    private String assignedOwner;

    private LeadStatus status;

    private String notes;

    private BigDecimal estimatedValue;

    private LocalDate expectedCloseDate;
}