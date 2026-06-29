package com.orque.crm.settings.metadata.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "crm_metadata_modules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrmMetadataModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private Boolean isCustom;
}
