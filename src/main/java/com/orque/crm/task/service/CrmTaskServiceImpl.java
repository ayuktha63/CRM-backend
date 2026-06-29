package com.orque.crm.task.service;

import com.orque.crm.common.UserContextHelper;
import com.orque.crm.enums.*;
import com.orque.crm.task.dto.*;
import com.orque.crm.task.entity.CrmTask;
import com.orque.crm.task.entity.TaskNotification;
import com.orque.crm.task.repository.CrmTaskRepository;
import com.orque.crm.task.repository.TaskNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.orque.crm.audit.service.AuditLogService;
import com.orque.crm.enums.AuditAction;
import com.orque.crm.enums.AuditModule;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CrmTaskServiceImpl implements CrmTaskService {

    private final CrmTaskRepository taskRepository;
    private final TaskNotificationRepository notificationRepository;
    private final AuditLogService auditLogService;
    @Override
    public CrmTaskResponse createTask(CreateCrmTaskRequest request) {

        String currentUser = UserContextHelper.currentUsername();
        CrmTask task = CrmTask.builder()
                .leadId(request.getLeadId())
                .contactId(request.getContactId())
                .relatedId(request.getRelatedId())
                .title(request.getTitle())
                .description(request.getDescription())
                .taskType(request.getTaskType() != null ? request.getTaskType() : TaskType.FOLLOW_UP)
                .priority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM)
                .status(request.getStatus() != null ? request.getStatus() : TaskStatus.PENDING)
                .assignedTo(currentUser)
                .relatedType(request.getRelatedType())
                .relatedName(request.getRelatedName())
                .dueDate(request.getDueDate())
                .notes(request.getNotes())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        CrmTask savedTask = taskRepository.save(task);
        auditLogService.createAudit(
                AuditAction.TASK_CREATED,
                AuditModule.TASK,
                "Task",
                savedTask.getId(),
                null,
                savedTask.getTitle(),
                "Task created: " + savedTask.getTitle(),
                savedTask.getAssignedTo(),
                null
        );

        TaskNotification notification =
                TaskNotification.builder()
                        .taskId(savedTask.getId())
                        .notificationType(NotificationType.IN_APP)
                        .status(NotificationStatus.PENDING)
                        .recipient(savedTask.getAssignedTo())
                        .message(
                                "Reminder: " + savedTask.getTitle()
                        )
                        .scheduledAt(savedTask.getDueDate())
                        .createdAt(LocalDateTime.now())
                        .build();

        notificationRepository.save(notification);

        return mapToResponse(savedTask);
    }

    @Override
    public CrmTaskResponse updateTask(
            Long id,
            UpdateCrmTaskRequest request
    ) {

        CrmTask task = taskRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Task not found"));

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setStatus(request.getStatus());
        task.setAssignedTo(request.getAssignedTo());
        task.setRelatedType(request.getRelatedType());
        task.setRelatedName(request.getRelatedName());
        task.setDueDate(request.getDueDate());
        task.setNotes(request.getNotes());
        task.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(
                taskRepository.save(task)
        );
    }

    @Override
    public CrmTaskResponse completeTask(Long id) {

        CrmTask task = taskRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Task not found"));

        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());

        CrmTask updatedTask = taskRepository.save(task);

        auditLogService.createAudit(
                AuditAction.TASK_COMPLETED,
                AuditModule.TASK,
                "Task",
                updatedTask.getId(),
                TaskStatus.PENDING.name(),
                TaskStatus.COMPLETED.name(),
                "Task completed: " + updatedTask.getTitle(),
                updatedTask.getAssignedTo(),
                null
        );

        return mapToResponse(updatedTask);

    }

    @Override
    public List<CrmTaskResponse> getAllTasks() {
        return taskRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public CrmTaskResponse getTaskById(Long id) {

        return taskRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() ->
                        new RuntimeException("Task not found"));
    }

    @Override
    public List<CrmTaskResponse> getPendingTasks() {

        return taskRepository.findByStatus(
                        TaskStatus.PENDING
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<CrmTaskResponse> getOverdueTasks() {

        return taskRepository
                .findByDueDateBeforeAndStatusNot(
                        LocalDateTime.now(),
                        TaskStatus.COMPLETED
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<TaskNotificationResponse> getNotifications() {

        return notificationRepository.findAll()
                .stream()
                .map(this::mapToNotificationResponse)
                .toList();
    }

    private CrmTaskResponse mapToResponse(
            CrmTask task
    ) {
        return CrmTaskResponse.builder()
                .id(task.getId())
                .leadId(task.getLeadId())
                .contactId(task.getContactId())
                .title(task.getTitle())
                .description(task.getDescription())
                .taskType(task.getTaskType())
                .priority(task.getPriority())
                .status(task.getStatus())
                .assignedTo(task.getAssignedTo())
                .relatedType(task.getRelatedType())
                .relatedName(task.getRelatedName())
                .dueDate(task.getDueDate())
                .notes(task.getNotes())
                .completedAt(task.getCompletedAt())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private TaskNotificationResponse mapToNotificationResponse(
            TaskNotification notification
    ) {
        return TaskNotificationResponse.builder()
                .id(notification.getId())
                .taskId(notification.getTaskId())
                .notificationType(notification.getNotificationType())
                .status(notification.getStatus())
                .recipient(notification.getRecipient())
                .message(notification.getMessage())
                .scheduledAt(notification.getScheduledAt())
                .sentAt(notification.getSentAt())
                .build();
    }
}