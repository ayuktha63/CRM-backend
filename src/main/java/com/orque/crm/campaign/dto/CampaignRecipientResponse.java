package com.orque.crm.campaign.dto;

import com.orque.crm.enums.CampaignRecipientStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CampaignRecipientResponse {

    private Long id;

    private Long campaignId;

    private String firstName;

    private String lastName;

    private String company;

    private String email;

    private CampaignRecipientStatus status;

    private LocalDateTime sentAt;

    private LocalDateTime openedAt;

    private LocalDateTime repliedAt;
}