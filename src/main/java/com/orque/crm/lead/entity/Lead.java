package com.orque.crm.lead.entity;

import com.orque.crm.enums.LeadSource;
import com.orque.crm.enums.LeadStatus;
import com.orque.crm.enums.PipelineStage;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** Organization this record belongs to. Populated automatically by the backend. */
    @Column(length = 36)
    private String organizationId;


    private Long contactId;

    @Column(nullable = false)
    private String fullName;

    private String company;

    @Column(nullable = false)
    private String email;

    private String phone;

    private String jobTitle;

    private String industry;

    private String website;

    private String address;

    private String country;

    private String state;

    private String city;

    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadSource leadSource;

    private String assignedOwner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadStatus status;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PipelineStage pipelineStage;
    @Column(columnDefinition = "TEXT")
    private String notes;

    private BigDecimal estimatedValue;

    private LocalDate expectedCloseDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}