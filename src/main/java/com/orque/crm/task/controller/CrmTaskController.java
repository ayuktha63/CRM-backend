package com.orque.crm.task.controller;

import com.orque.crm.task.dto.CreateCrmTaskRequest;
import com.orque.crm.task.dto.CrmTaskResponse;
import com.orque.crm.task.dto.TaskNotificationResponse;
import com.orque.crm.task.dto.UpdateCrmTaskRequest;
import com.orque.crm.task.service.CrmTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class CrmTaskController {

    private final CrmTaskService taskService;

    @PostMapping
    public CrmTaskResponse createTask(
            @RequestBody CreateCrmTaskRequest request
    ) {
        return taskService.createTask(request);
    }

    @GetMapping
    public List<CrmTaskResponse> getAllTasks() {
        return taskService.getAllTasks();
    }

    @GetMapping("/{id}")
    public CrmTaskResponse getTaskById(
            @PathVariable Long id
    ) {
        return taskService.getTaskById(id);
    }

    @PutMapping("/{id}")
    public CrmTaskResponse updateTask(
            @PathVariable Long id,
            @RequestBody UpdateCrmTaskRequest request
    ) {
        return taskService.updateTask(id, request);
    }

    @PutMapping("/{id}/complete")
    public CrmTaskResponse completeTask(
            @PathVariable Long id
    ) {
        return taskService.completeTask(id);
    }

    @GetMapping("/pending")
    public List<CrmTaskResponse> getPendingTasks() {
        return taskService.getPendingTasks();
    }

    @GetMapping("/overdue")
    public List<CrmTaskResponse> getOverdueTasks() {
        return taskService.getOverdueTasks();
    }

    @GetMapping("/notifications")
    public List<TaskNotificationResponse> getNotifications() {
        return taskService.getNotifications();
    }
}