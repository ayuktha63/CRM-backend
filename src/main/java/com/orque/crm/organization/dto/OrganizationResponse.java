package com.orque.crm.organization.dto;

import com.orque.crm.enums.OrganizationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrganizationResponse {
    private String id;
    private String organizationCode;
    private String organizationName;
    private String legalName;
    private String email;
    private String phone;
    private String address;
    private String country;
    private String timezone;
    private String currency;
    private String gstin;
    private String companyTagline;
    private String bankName;
    private String bankAccountNumber;
    private String bankIfsc;
    private String upiId;
    private Integer paymentTermsDays;
    private String lateFeeText;
    private OrganizationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // populated on demand
    private Long userCount;
    private LicenseSummary license;

    @Data
    @Builder
    public static class LicenseSummary {
        private String status;
        private java.time.LocalDate endDate;
        private int daysRemaining;
        private int maxUsers;
        private int concurrentUsers;
    }
}
