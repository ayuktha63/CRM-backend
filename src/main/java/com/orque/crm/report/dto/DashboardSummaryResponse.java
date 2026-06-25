package com.orque.crm.report.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DashboardSummaryResponse {

    private Long totalContacts;

    private Long totalLeads;

    private Long hotLeads;

    private Long tasksDueToday;

    private BigDecimal revenueGenerated;

    private BigDecimal pipelineValue;

    private Long totalCampaigns;

    private Long emailsSent;

    private Long emailsOpened;

    private Long emailsReplied;
}