package com.orque.crm.report.dto;

import com.orque.crm.enums.DashboardWidgetType;
import lombok.Data;

@Data
public class DashboardPreferenceRequest {

    private DashboardWidgetType widgetType;

    private Integer displayOrder;

    private Boolean visible;
}