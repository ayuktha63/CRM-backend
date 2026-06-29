package com.orque.crm.settings.metadata.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "crm_custom_field_data", indexes = {
    @Index(name = "idx_cf_module_record", columnList = "moduleName, recordId"),
    @Index(name = "idx_cf_field_name", columnList = "fieldName")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrmCustomFieldData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String moduleName;

    @Column(nullable = false)
    private Long recordId;

    @Column(nullable = false)
    private String fieldName;

    @Column(columnDefinition = "TEXT")
    private String fieldValue;
}
