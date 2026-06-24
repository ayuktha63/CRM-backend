package com.orque.crm.pipeline.service;

import com.orque.crm.enums.PipelineStage;
import com.orque.crm.lead.dto.LeadResponse;
import com.orque.crm.lead.entity.Lead;
import com.orque.crm.lead.repository.LeadRepository;
import com.orque.crm.pipeline.dto.*;
import com.orque.crm.pipeline.entity.PipelineStageHistory;
import com.orque.crm.pipeline.repository.PipelineStageHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PipelineServiceImpl implements PipelineService {

    private final LeadRepository leadRepository;
    private final PipelineStageHistoryRepository historyRepository;

    @Override
    public PipelineBoardResponse getKanbanBoard() {

        List<PipelineStageColumnResponse> columns =
                Arrays.stream(PipelineStage.values())
                        .map(stage -> {
                            List<Lead> leads =
                                    leadRepository.findByPipelineStage(stage);

                            BigDecimal totalValue = leads.stream()
                                    .map(Lead::getEstimatedValue)
                                    .filter(value -> value != null)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                            return PipelineStageColumnResponse.builder()
                                    .stage(stage)
                                    .leadCount((long) leads.size())
                                    .totalEstimatedValue(totalValue)
                                    .leads(
                                            leads.stream()
                                                    .map(this::mapToLeadResponse)
                                                    .toList()
                                    )
                                    .build();
                        })
                        .toList();

        return PipelineBoardResponse.builder()
                .columns(columns)
                .build();
    }

    @Override
    public LeadResponse moveLeadStage(
            Long leadId,
            MoveLeadStageRequest request
    ) {

        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new RuntimeException("Lead not found"));

        PipelineStage previousStage = lead.getPipelineStage();

        if (previousStage == null) {
            previousStage = PipelineStage.NEW;
        }

        lead.setPipelineStage(request.getNewStage());
        lead.setUpdatedAt(LocalDateTime.now());

        Lead updatedLead = leadRepository.save(lead);

        PipelineStageHistory history =
                PipelineStageHistory.builder()
                        .leadId(updatedLead.getId())
                        .previousStage(previousStage)
                        .newStage(request.getNewStage())
                        .movedBy(request.getMovedBy())
                        .remarks(request.getRemarks())
                        .movedAt(LocalDateTime.now())
                        .build();

        historyRepository.save(history);

        return mapToLeadResponse(updatedLead);
    }

    @Override
    public List<PipelineStageHistoryResponse> getLeadStageHistory(
            Long leadId
    ) {
        return historyRepository.findByLeadIdOrderByMovedAtDesc(leadId)
                .stream()
                .map(this::mapToHistoryResponse)
                .toList();
    }

    @Override
    public PipelineHealthResponse getPipelineHealth() {

        List<Lead> allLeads = leadRepository.findAll();

        long totalLeads = allLeads.size();

        long wonLeads = allLeads.stream()
                .filter(lead -> lead.getPipelineStage() == PipelineStage.WON)
                .count();

        long lostLeads = allLeads.stream()
                .filter(lead -> lead.getPipelineStage() == PipelineStage.LOST)
                .count();

        long openLeads = allLeads.stream()
                .filter(lead ->
                        lead.getPipelineStage() != PipelineStage.WON &&
                                lead.getPipelineStage() != PipelineStage.LOST
                )
                .count();

        BigDecimal totalOpenOpportunity = allLeads.stream()
                .filter(lead ->
                        lead.getPipelineStage() != PipelineStage.WON &&
                                lead.getPipelineStage() != PipelineStage.LOST
                )
                .map(Lead::getEstimatedValue)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWonRevenue = allLeads.stream()
                .filter(lead -> lead.getPipelineStage() == PipelineStage.WON)
                .map(Lead::getEstimatedValue)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double winRate = 0.0;

        if (totalLeads > 0) {
            winRate = BigDecimal.valueOf(wonLeads)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(
                            BigDecimal.valueOf(totalLeads),
                            2,
                            RoundingMode.HALF_UP
                    )
                    .doubleValue();
        }

        return PipelineHealthResponse.builder()
                .totalLeads(totalLeads)
                .openLeads(openLeads)
                .wonLeads(wonLeads)
                .lostLeads(lostLeads)
                .totalOpenOpportunity(totalOpenOpportunity)
                .totalWonRevenue(totalWonRevenue)
                .winRate(winRate)
                .build();
    }

    private PipelineStageHistoryResponse mapToHistoryResponse(
            PipelineStageHistory history
    ) {
        return PipelineStageHistoryResponse.builder()
                .id(history.getId())
                .leadId(history.getLeadId())
                .previousStage(history.getPreviousStage())
                .newStage(history.getNewStage())
                .movedBy(history.getMovedBy())
                .remarks(history.getRemarks())
                .movedAt(history.getMovedAt())
                .build();
    }

    private LeadResponse mapToLeadResponse(Lead lead) {
        return LeadResponse.builder()
                .id(lead.getId())
                .contactId(lead.getContactId())
                .fullName(lead.getFullName())
                .company(lead.getCompany())
                .email(lead.getEmail())
                .phone(lead.getPhone())
                .jobTitle(lead.getJobTitle())
                .industry(lead.getIndustry())
                .website(lead.getWebsite())
                .address(lead.getAddress())
                .country(lead.getCountry())
                .state(lead.getState())
                .city(lead.getCity())
                .tags(lead.getTags())
                .leadSource(lead.getLeadSource())
                .assignedOwner(lead.getAssignedOwner())
                .status(lead.getStatus())
                .pipelineStage(lead.getPipelineStage())
                .notes(lead.getNotes())
                .estimatedValue(lead.getEstimatedValue())
                .expectedCloseDate(lead.getExpectedCloseDate())
                .createdAt(lead.getCreatedAt())
                .updatedAt(lead.getUpdatedAt())
                .build();
    }
}