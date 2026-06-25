package com.orque.crm.campaign.dto;

import com.orque.crm.enums.CampaignRecipientStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CampaignHistoryResponse {

    private Long id;

    private String toEmail;

    private String subject;

    private CampaignRecipientStatus status;

    private LocalDateTime sentAt;
}