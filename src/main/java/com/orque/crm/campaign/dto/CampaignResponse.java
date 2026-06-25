package com.orque.crm.campaign.dto;

import com.orque.crm.enums.CampaignStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CampaignResponse {

    private Long id;

    private String campaignName;

    private String subjectLine;

    private String emailBody;

    private Long mailboxId;

    private CampaignStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime launchedAt;

    private LocalDateTime completedAt;
}