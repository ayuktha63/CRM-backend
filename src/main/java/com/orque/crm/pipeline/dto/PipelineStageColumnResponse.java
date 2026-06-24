package com.orque.crm.pipeline.dto;

import com.orque.crm.enums.PipelineStage;
import com.orque.crm.lead.dto.LeadResponse;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineStageColumnResponse {

    private PipelineStage stage;

    private Long leadCount;

    private BigDecimal totalEstimatedValue;

    private List<LeadResponse> leads;
}