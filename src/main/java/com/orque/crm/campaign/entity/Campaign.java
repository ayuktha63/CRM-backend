package com.orque.crm.campaign.entity;

import com.orque.crm.enums.CampaignStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "campaigns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String campaignName;

    private String subjectLine;

    @Column(columnDefinition = "TEXT")
    private String emailBody;

    private Long mailboxId;

    @Enumerated(EnumType.STRING)
    private CampaignStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime launchedAt;

    private LocalDateTime completedAt;
}