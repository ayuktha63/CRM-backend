package com.orque.crm.campaign.repository;

import com.orque.crm.campaign.entity.CampaignRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaignRecipientRepository
        extends JpaRepository<CampaignRecipient, Long> {

    List<CampaignRecipient> findByCampaignId(Long campaignId);
}