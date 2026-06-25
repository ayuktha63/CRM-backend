package com.orque.crm.report.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReportSummaryResponse {

    private String reportName;

    private Long totalRecords;

    private String generatedAt;

    private Object data;
}