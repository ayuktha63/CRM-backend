package com.orque.crm.license.entity;

import com.orque.crm.enums.LicenseStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "crm_licenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrmLicense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String organizationId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String licenseKey;   // raw encrypted key (for re-display / replace)

    private String licenseName;
    private String orgCode;
    private String productName;

    private LocalDate startDate;
    private LocalDate endDate;

    @Builder.Default
    private Integer gracePeriodDays = 30;

    @Builder.Default
    private Integer maximumUsers = 10;

    @Builder.Default
    private Integer concurrentUsers = 5;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private LicenseStatus status = LicenseStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String featuresJson;

    private String licenseHash;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = LicenseStatus.ACTIVE;
        if (gracePeriodDays == null) gracePeriodDays = 30;
        if (maximumUsers == null) maximumUsers = 10;
        if (concurrentUsers == null) concurrentUsers = 5;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
