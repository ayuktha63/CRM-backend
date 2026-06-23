package com.orque.crm.lead.entity;

import com.orque.crm.enums.LeadActivityType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "lead_activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long leadId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadActivityType activityType;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDateTime createdAt;
}