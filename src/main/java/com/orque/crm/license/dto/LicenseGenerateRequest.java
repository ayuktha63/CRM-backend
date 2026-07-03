package com.orque.crm.license.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LicenseGenerateRequest {

    private String productName = "CRM";

    @NotBlank
    private String orgCode;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @Min(0)
    private int gracePeriodDays = 30;

    @Min(1)
    private int maxUsers = 10;

    @Min(1)
    private int concurrentUsers = 5;
}
