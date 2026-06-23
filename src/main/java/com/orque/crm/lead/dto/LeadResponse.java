package com.orque.crm.lead.dto;

import com.orque.crm.enums.LeadSource;
import com.orque.crm.enums.LeadStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadResponse {

    private Long id;
    private Long contactId;

    private String fullName;
    private String company;
    private String email;
    private String phone;
    private String jobTitle;
    private String industry;
    private String website;
    private String address;
    private String country;
    private String state;
    private String city;
    private String tags;

    private LeadSource leadSource;
    private String assignedOwner;
    private LeadStatus status;
    private String notes;
    private BigDecimal estimatedValue;
    private LocalDate expectedCloseDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}