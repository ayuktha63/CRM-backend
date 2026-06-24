package com.orque.crm.task.dto;

import com.orque.crm.enums.TaskPriority;
import com.orque.crm.enums.TaskType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCrmTaskRequest {

    private Long leadId;

    private Long contactId;

    private String title;

    private String description;

    private TaskType taskType;

    private TaskPriority priority;

    private String assignedTo;

    private LocalDateTime dueDate;

    private String notes;
}