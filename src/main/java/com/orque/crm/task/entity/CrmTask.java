package com.orque.crm.task.entity;

import com.orque.crm.enums.TaskPriority;
import com.orque.crm.enums.TaskStatus;
import com.orque.crm.enums.TaskType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "crm_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrmTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** Organization this record belongs to. Populated automatically by the backend. */
    @Column(length = 36)
    private String organizationId;


    private Long leadId;

    private Long contactId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskType taskType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    private String assignedTo;

    private String relatedType;

    private String relatedName;

    private Long relatedId;

    private LocalDateTime dueDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}