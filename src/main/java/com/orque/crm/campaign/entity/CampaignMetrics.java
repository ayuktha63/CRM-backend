package com.orque.crm.campaign.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "campaign_metrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long campaignId;

    private Integer totalRecipients;

    private Integer sentCount;

    private Integer failedCount;

    private Integer deliveredCount;

    private Integer openedCount;

    private Integer repliedCount;
}