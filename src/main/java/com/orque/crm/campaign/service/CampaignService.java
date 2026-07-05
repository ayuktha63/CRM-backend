package com.orque.crm.campaign.service;

import com.orque.crm.campaign.dto.*;

import java.util.List;

public interface CampaignService {

    CampaignResponse createCampaign(
            CreateCampaignRequest request
    );

    List<CampaignResponse> getAllCampaigns();

    CampaignResponse getCampaign(
            Long campaignId
    );

    CampaignResponse updateCampaign(
            Long campaignId,
            CreateCampaignRequest request
    );

    CampaignRecipientResponse addRecipient(
            Long campaignId,
            AddRecipientsRequest request
    );

    List<CampaignRecipientResponse> getRecipients(
            Long campaignId
    );

    void launchCampaign(
            Long campaignId
    );

    List<CampaignHistoryResponse> getCampaignHistory(
            Long campaignId
    );

    CampaignMetricsResponse getCampaignMetrics(
            Long campaignId
    );
}