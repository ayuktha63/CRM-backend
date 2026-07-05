package com.orque.crm.tax.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One row per tenant. `configJson` is the full tax-rule snapshot for the chosen
 * country (components for same-state/different-state/flat cases + rates) — the same
 * shape the frontend's static country list uses, copied in wholesale when the admin
 * picks a country. TaxCalculationService reads only this JSON, never a country name,
 * so adding a new country is a data change (frontend JSON + one save), not a code change.
 */
@Entity
@Table(name = "organization_tax_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationTaxSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String organizationId;

    private String countryCode;
    private String countryName;
    private String taxSystem;
    private String registrationLabel;
    private String registrationNumber;
    private String businessState;
    private Boolean requiresBusinessState;

    @Column(columnDefinition = "TEXT")
    private String configJson;

    @Builder.Default
    private Boolean enabled = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (enabled == null) enabled = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
