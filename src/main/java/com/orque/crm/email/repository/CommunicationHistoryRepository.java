package com.orque.crm.email.repository;

import com.orque.crm.email.entity.CommunicationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunicationHistoryRepository
        extends JpaRepository<CommunicationHistory, Long> {

    List<CommunicationHistory> findByLeadIdOrderByActivityAtDesc(
            Long leadId
    );

    List<CommunicationHistory> findByContactIdOrderByActivityAtDesc(
            Long contactId
    );
}