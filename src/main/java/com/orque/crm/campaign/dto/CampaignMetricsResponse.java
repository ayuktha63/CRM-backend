package com.orque.crm.campaign.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CampaignMetricsResponse {

    private Integer totalRecipients;

    private Integer sentCount;

    private Integer failedCount;

    private Integer deliveredCount;

    private Integer openedCount;

    private Integer repliedCount;
}