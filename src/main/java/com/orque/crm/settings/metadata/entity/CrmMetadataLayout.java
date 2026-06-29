package com.orque.crm.settings.metadata.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "crm_metadata_layouts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrmMetadataLayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String moduleName;

    @Column(nullable = false)
    private String name;

    private String roleName; // E.g., SYSTEM_ADMIN, APPROVER, REQUESTER, VIEWER

    @Column(nullable = false, columnDefinition = "TEXT")
    private String layoutDefinition; // JSON string mapping fields, sections, tabs
}
