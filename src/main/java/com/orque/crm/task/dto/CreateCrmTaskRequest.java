package com.orque.crm.task.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.orque.crm.enums.TaskPriority;
import com.orque.crm.enums.TaskStatus;
import com.orque.crm.enums.TaskType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateCrmTaskRequest {

    private Long leadId;

    private Long contactId;

    private Long relatedId;

    private String title;

    private String description;

    private TaskType taskType;

    private TaskPriority priority;

    private TaskStatus status;

    private String assignedTo;

    private String relatedType;

    private String relatedName;

    private LocalDateTime dueDate;

    private String notes;
}