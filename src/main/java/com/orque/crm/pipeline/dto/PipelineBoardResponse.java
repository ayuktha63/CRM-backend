package com.orque.crm.pipeline.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineBoardResponse {

    private List<PipelineStageColumnResponse> columns;
}