package com.orque.crm.task.controller;

import com.orque.crm.auth.entity.User;
import com.orque.crm.common.UserContextHelper;
import com.orque.crm.task.dto.CreateCrmTaskRequest;
import com.orque.crm.task.dto.CrmTaskResponse;
import com.orque.crm.task.dto.TaskNotificationResponse;
import com.orque.crm.task.dto.UpdateCrmTaskRequest;
import com.orque.crm.task.entity.CrmTask;
import com.orque.crm.task.repository.CrmTaskRepository;
import com.orque.crm.task.service.CrmTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@CrossOrigin
public class CrmTaskController {

    private final CrmTaskService taskService;
    private final CrmTaskRepository taskRepository;

    @PostMapping
    public CrmTaskResponse createTask(@RequestBody CreateCrmTaskRequest request) {
        request.setAssignedTo(UserContextHelper.currentUsername());
        return taskService.createTask(request);
    }

    @GetMapping
    public List<CrmTaskResponse> getAllTasks() {
        return taskService.getAllTasks().stream()
                .filter(t -> UserContextHelper.canAccess(t.getAssignedTo()))
                .toList();
    }

    @GetMapping("/{id}")
    public CrmTaskResponse getTaskById(@PathVariable Long id) {
        CrmTaskResponse task = taskService.getTaskById(id);
        UserContextHelper.assertAccess(task.getAssignedTo());
        return task;
    }

    @PutMapping("/{id}")
    public CrmTaskResponse updateTask(@PathVariable Long id, @RequestBody UpdateCrmTaskRequest request) {
        CrmTaskResponse existing = taskService.getTaskById(id);
        UserContextHelper.assertAccess(existing.getAssignedTo());
        // Preserve original owner — edit does not reassign
        request.setAssignedTo(existing.getAssignedTo());
        return taskService.updateTask(id, request);
    }

    @PutMapping("/{id}/complete")
    public CrmTaskResponse completeTask(@PathVariable Long id) {
        CrmTaskResponse existing = taskService.getTaskById(id);
        UserContextHelper.assertAccess(existing.getAssignedTo());
        return taskService.completeTask(id);
    }

    @GetMapping("/pending")
    public List<CrmTaskResponse> getPendingTasks() {
        return taskService.getPendingTasks().stream()
                .filter(t -> UserContextHelper.canAccess(t.getAssignedTo()))
                .toList();
    }

    @GetMapping("/overdue")
    public List<CrmTaskResponse> getOverdueTasks() {
        return taskService.getOverdueTasks().stream()
                .filter(t -> UserContextHelper.canAccess(t.getAssignedTo()))
                .toList();
    }

    @GetMapping("/notifications")
    public List<TaskNotificationResponse> getNotifications() {
        return taskService.getNotifications();
    }

    @GetMapping("/by-record")
    public ResponseEntity<List<CrmTask>> getByRecord(
            @RequestParam String type,
            @RequestParam Long id) {
        List<CrmTask> tasks = taskRepository.findByRelatedTypeIgnoreCaseAndRelatedId(type, id);
        return ResponseEntity.ok(tasks.stream()
                .filter(t -> UserContextHelper.canAccess(t.getAssignedTo()))
                .toList());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTask(@PathVariable Long id) {
        CrmTaskResponse existing = taskService.getTaskById(id);
        UserContextHelper.assertAccess(existing.getAssignedTo());
        taskRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Task deleted successfully"));
    }
}
