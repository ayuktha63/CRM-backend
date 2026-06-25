package com.orque.crm.report.entity;

import com.orque.crm.enums.DashboardWidgetType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dashboard_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Enumerated(EnumType.STRING)
    private DashboardWidgetType widgetType;

    private Integer displayOrder;

    private Boolean visible;
}