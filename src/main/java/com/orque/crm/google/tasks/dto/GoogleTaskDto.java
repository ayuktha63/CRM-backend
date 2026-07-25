package com.orque.crm.google.tasks.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GoogleTaskDto {
    private String id;
    private String taskListId;
    private String title;
    private String notes;
    private String due;      // ISO date, e.g. 2026-08-01
    private String status;   // "needsAction" | "completed"
    private String completedAt;
}
