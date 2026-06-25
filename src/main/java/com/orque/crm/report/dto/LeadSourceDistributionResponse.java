package com.orque.crm.report.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LeadSourceDistributionResponse {

    private String source;

    private Long count;
}