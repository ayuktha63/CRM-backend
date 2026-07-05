package com.orque.crm.lead.repository;

import com.orque.crm.lead.entity.LeadActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeadActivityRepository extends JpaRepository<LeadActivity, Long> {

    List<LeadActivity> findByLeadIdOrderByCreatedAtDesc(Long leadId);

    void deleteByLeadId(Long leadId);
}