package com.orque.crm.feature.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** Organization this record belongs to. Populated automatically by the backend. */
    @Column(length = 36)
    private String organizationId;


    @Column(nullable = false)
    private String companyName;

    private String industry;
    private String phone;
    private String website;
    private Integer employees;
    private String annualRevenue;
    private String country;
    private String status;
    private String owner;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
