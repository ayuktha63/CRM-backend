package com.orque.crm.report.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crm_dashboards")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrmDashboard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String shareType; // PUBLIC, PRIVATE, TEAM

    @Column(columnDefinition = "TEXT")
    private String layoutConfig; // JSON mapping grid cells and widgets configuration

    private String createdBy;

    /** Tenant this dashboard belongs to. Populated automatically by the backend. */
    @Column(length = 36)
    private String organizationId;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.shareType == null) {
            this.shareType = "PRIVATE";
        }
    }
}
