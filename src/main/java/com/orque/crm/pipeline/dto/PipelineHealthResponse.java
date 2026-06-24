package com.orque.crm.pipeline.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineHealthResponse {

    private Long totalLeads;

    private Long openLeads;

    private Long wonLeads;

    private Long lostLeads;

    private BigDecimal totalOpenOpportunity;

    private BigDecimal totalWonRevenue;

    private Double winRate;
}