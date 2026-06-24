package com.orque.crm.task.service;

import com.orque.crm.task.dto.CreateCrmTaskRequest;
import com.orque.crm.task.dto.CrmTaskResponse;
import com.orque.crm.task.dto.TaskNotificationResponse;
import com.orque.crm.task.dto.UpdateCrmTaskRequest;

import java.util.List;

public interface CrmTaskService {

    CrmTaskResponse createTask(
            CreateCrmTaskRequest request
    );

    CrmTaskResponse updateTask(
            Long id,
            UpdateCrmTaskRequest request
    );

    CrmTaskResponse completeTask(
            Long id
    );

    List<CrmTaskResponse> getAllTasks();

    CrmTaskResponse getTaskById(
            Long id
    );

    List<CrmTaskResponse> getPendingTasks();

    List<CrmTaskResponse> getOverdueTasks();

    List<TaskNotificationResponse> getNotifications();
}