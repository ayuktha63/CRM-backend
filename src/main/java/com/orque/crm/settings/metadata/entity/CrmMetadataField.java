package com.orque.crm.settings.metadata.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "crm_metadata_fields")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrmMetadataField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String moduleName;

    /** Tenant this field belongs to. Nullable only for rows that predate this column
     *  (legacy data) — new fields always get the creating user's organizationId. */
    @Column(length = 36)
    private String organizationId;

    private String createdBy;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private String type; // TEXT, NUMBER, CURRENCY, PHONE, EMAIL, DATE, DATETIME, DROPDOWN, CHECKBOX, FORMULA, AUTO_NUMBER, LOOKUP

    @Column(nullable = false)
    private Boolean isRequired;

    @Column(nullable = false)
    private Boolean isReadonly;

    @Column(columnDefinition = "TEXT")
    private String selectOptions; // Comma-separated options

    @Column(columnDefinition = "TEXT")
    private String formulaExpression; // For formula evaluation

    private String lookupTargetModule; // Target module name for LOOKUP type
}
