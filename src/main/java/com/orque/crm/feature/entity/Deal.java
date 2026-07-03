package com.orque.crm.feature.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "deals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** Organization this record belongs to. Populated automatically by the backend. */
    @Column(length = 36)
    private String organizationId;


    @Column(nullable = false)
    private String dealName;

    @Column(nullable = false)
    private String account;

    private String contact;
    private BigDecimal amount;
    private String stage;
    private Integer probability;
    private LocalDate expectedCloseDate;
    private String assignedTo;
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        updateStatusFromStage();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        updateStatusFromStage();
    }

    private void updateStatusFromStage() {
        if ("Closed Won".equalsIgnoreCase(stage)) {
            status = "Closed Won";
        } else if ("Closed Lost".equalsIgnoreCase(stage)) {
            status = "Closed Lost";
        } else {
            status = stage;
        }
    }
}
