package com.orque.crm.report.dto;

import com.orque.crm.enums.DashboardWidgetType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardPreferenceResponse {

    private DashboardWidgetType widgetType;

    private Integer displayOrder;

    private Boolean visible;
}