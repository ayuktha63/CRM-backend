package com.orque.crm.report.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConversionFunnelResponse {

    private String stage;

    private Long count;
}