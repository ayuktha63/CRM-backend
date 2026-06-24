package com.orque.crm.pipeline.controller;

import com.orque.crm.lead.dto.LeadResponse;
import com.orque.crm.pipeline.dto.*;
import com.orque.crm.pipeline.service.PipelineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pipeline")
@RequiredArgsConstructor
public class PipelineController {

    private final PipelineService pipelineService;

    @GetMapping("/board")
    public PipelineBoardResponse getKanbanBoard() {
        return pipelineService.getKanbanBoard();
    }

    @PutMapping("/leads/{leadId}/stage")
    public LeadResponse moveLeadStage(
            @PathVariable Long leadId,
            @Valid @RequestBody MoveLeadStageRequest request
    ) {
        return pipelineService.moveLeadStage(leadId, request);
    }

    @GetMapping("/leads/{leadId}/history")
    public List<PipelineStageHistoryResponse> getLeadStageHistory(
            @PathVariable Long leadId
    ) {
        return pipelineService.getLeadStageHistory(leadId);
    }

    @GetMapping("/health")
    public PipelineHealthResponse getPipelineHealth() {
        return pipelineService.getPipelineHealth();
    }
}