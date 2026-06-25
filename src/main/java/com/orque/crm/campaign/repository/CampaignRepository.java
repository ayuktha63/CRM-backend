package com.orque.crm.campaign.repository;

import com.orque.crm.campaign.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {
}