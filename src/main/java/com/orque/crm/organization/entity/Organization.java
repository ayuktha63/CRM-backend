package com.orque.crm.organization.entity;

import com.orque.crm.enums.OrganizationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true)
    private String organizationCode;

    @Column(nullable = false)
    private String organizationName;

    private String legalName;
    private String email;
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String country;
    private String timezone;
    private String currency;

    // Billing/invoice details — printed on this tenant's own Quote/Invoice PDFs so
    // each tenant's documents show their own company info instead of Orque's.
    private String gstin;
    private String companyTagline;
    private String bankName;
    private String bankAccountNumber;
    private String bankIfsc;
    private String upiId;
    private Integer paymentTermsDays;
    private String lateFeeText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID().toString();
        if (status == null) status = OrganizationStatus.ACTIVE;
        if (currency == null) currency = "INR";
        if (timezone == null) timezone = "Asia/Kolkata";
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
