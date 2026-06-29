package com.orque.crm.pipeline.controller;

import com.orque.crm.common.UserContextHelper;
import com.orque.crm.lead.dto.LeadResponse;
import com.orque.crm.pipeline.dto.*;
import com.orque.crm.pipeline.service.PipelineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pipeline")
@RequiredArgsConstructor
public class PipelineController {

    private final PipelineService pipelineService;

    @GetMapping("/board")
    public PipelineBoardResponse getKanbanBoard() {
        PipelineBoardResponse board = pipelineService.getKanbanBoard();
        if (UserContextHelper.isAdmin()) return board;

        List<PipelineStageColumnResponse> filtered = board.getColumns().stream()
                .map(col -> {
                    List<LeadResponse> visibleLeads = col.getLeads().stream()
                            .filter(l -> UserContextHelper.canAccess(l.getAssignedOwner()))
                            .toList();
                    BigDecimal total = visibleLeads.stream()
                            .map(l -> l.getEstimatedValue() != null ? l.getEstimatedValue() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return PipelineStageColumnResponse.builder()
                            .stage(col.getStage())
                            .leads(visibleLeads)
                            .leadCount((long) visibleLeads.size())
                            .totalEstimatedValue(total)
                            .build();
                })
                .toList();
        return PipelineBoardResponse.builder().columns(filtered).build();
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
