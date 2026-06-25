package com.orque.crm.campaign.repository;

import com.orque.crm.campaign.entity.CampaignEmailHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaignEmailHistoryRepository
        extends JpaRepository<CampaignEmailHistory, Long> {

    List<CampaignEmailHistory> findByCampaignId(Long campaignId);

    List<CampaignEmailHistory> findByToEmailContainingIgnoreCase(String email);
}