package com.orque.crm.campaign.repository;

import com.orque.crm.campaign.entity.CampaignMetrics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CampaignMetricsRepository
        extends JpaRepository<CampaignMetrics, Long> {

    Optional<CampaignMetrics> findByCampaignId(Long campaignId);
}