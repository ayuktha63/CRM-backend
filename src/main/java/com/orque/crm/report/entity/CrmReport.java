package com.orque.crm.report.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crm_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrmReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String moduleName;

    private String relatedModules; // Comma-separated list

    @Column(columnDefinition = "TEXT")
    private String columns; // JSON format list of fields to display

    @Column(columnDefinition = "TEXT")
    private String groupBy; // JSON format group by parameters

    @Column(columnDefinition = "TEXT")
    private String aggregations; // JSON aggregates config: field -> function (SUM, AVG)

    @Column(columnDefinition = "TEXT")
    private String filters; // JSON representation of filter queries

    @Column(columnDefinition = "TEXT")
    private String chartConfig; // JSON configuration of chart (type, x, y fields)

    private String scheduledTime; // Cron expression for email delivery

    @Column(nullable = false)
    private String shareType; // PUBLIC, PRIVATE, TEAM

    private String createdBy;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.shareType == null) {
            this.shareType = "PRIVATE";
        }
    }
}
