package com.orque.crm.pipeline.service;

import com.orque.crm.lead.dto.LeadResponse;
import com.orque.crm.pipeline.dto.*;

import java.util.List;

public interface PipelineService {

    PipelineBoardResponse getKanbanBoard();

    LeadResponse moveLeadStage(
            Long leadId,
            MoveLeadStageRequest request
    );

    List<PipelineStageHistoryResponse> getLeadStageHistory(
            Long leadId
    );

    PipelineHealthResponse getPipelineHealth();
}