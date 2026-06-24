package com.orque.crm.pipeline.dto;

import com.orque.crm.enums.PipelineStage;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineStageHistoryResponse {

    private Long id;

    private Long leadId;

    private PipelineStage previousStage;

    private PipelineStage newStage;

    private String movedBy;

    private String remarks;

    private LocalDateTime movedAt;
}