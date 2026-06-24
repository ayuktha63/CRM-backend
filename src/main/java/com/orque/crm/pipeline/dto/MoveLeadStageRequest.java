package com.orque.crm.pipeline.dto;

import com.orque.crm.enums.PipelineStage;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoveLeadStageRequest {

    @NotNull(message = "New stage is required")
    private PipelineStage newStage;

    private String movedBy;

    private String remarks;
}