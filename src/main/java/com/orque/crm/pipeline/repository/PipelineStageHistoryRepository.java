package com.orque.crm.pipeline.repository;

import com.orque.crm.pipeline.entity.PipelineStageHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PipelineStageHistoryRepository
        extends JpaRepository<PipelineStageHistory, Long> {

    List<PipelineStageHistory>
    findByLeadIdOrderByMovedAtDesc(Long leadId);
}