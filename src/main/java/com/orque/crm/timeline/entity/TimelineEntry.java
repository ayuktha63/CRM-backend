package com.orque.crm.timeline.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crm_timeline")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimelineEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String moduleName;

    @Column(nullable = false)
    private Long recordId;

    @Column(nullable = false)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String performedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
