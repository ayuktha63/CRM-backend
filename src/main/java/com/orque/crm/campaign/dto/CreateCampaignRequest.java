package com.orque.crm.campaign.dto;

import com.orque.crm.enums.CampaignStatus;
import lombok.Data;

@Data
public class CreateCampaignRequest {

    private String campaignName;

    private String subjectLine;

    private String emailBody;

    private Long mailboxId;

    private CampaignStatus status;
}