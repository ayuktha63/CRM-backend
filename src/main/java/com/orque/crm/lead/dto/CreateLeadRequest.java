package com.orque.crm.lead.dto;

import com.orque.crm.enums.LeadSource;
import com.orque.crm.enums.LeadStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLeadRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String company;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
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
}