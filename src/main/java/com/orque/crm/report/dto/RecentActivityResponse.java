package com.orque.crm.report.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RecentActivityResponse {

    private String module;

    private String activityType;

    private String description;

    private LocalDateTime activityAt;
}