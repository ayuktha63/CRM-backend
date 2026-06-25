package com.orque.crm.email.entity;

import com.orque.crm.enums.EmailActivityType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "communication_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunicationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long contactId;

    private Long leadId;

    private Long emailMessageId;

    @Enumerated(EnumType.STRING)
    private EmailActivityType activityType;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDateTime activityAt;
}