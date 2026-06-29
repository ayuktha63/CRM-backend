package com.orque.crm.task.dto;

import com.orque.crm.enums.TaskPriority;
import com.orque.crm.enums.TaskStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCrmTaskRequest {

    private String title;

    private String description;

    private TaskPriority priority;

    private TaskStatus status;

    private String assignedTo;

    private String relatedType;

    private String relatedName;

    private LocalDateTime dueDate;

    private String notes;
}