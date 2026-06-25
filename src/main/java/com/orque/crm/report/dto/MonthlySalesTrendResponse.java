package com.orque.crm.report.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MonthlySalesTrendResponse {

    private String month;

    private Long leadsCreated;

    private BigDecimal estimatedValue;
}