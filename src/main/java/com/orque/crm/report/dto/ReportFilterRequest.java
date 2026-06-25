package com.orque.crm.report.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ReportFilterRequest {

    private LocalDate startDate;

    private LocalDate endDate;

    private String campaignStatus;

    private String leadSource;

    private String leadStatus;
}