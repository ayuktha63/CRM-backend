package com.orque.crm.google.tasks;

import com.orque.crm.common.UserContextHelper;
import com.orque.crm.google.tasks.dto.GoogleTaskDto;
import com.orque.crm.google.tasks.dto.GoogleTaskListDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/google/tasks")
@RequiredArgsConstructor
public class GoogleTasksController {

    private final GoogleTasksService tasksService;

    @GetMapping("/lists")
    public ResponseEntity<List<GoogleTaskListDto>> listTaskLists() {
        return ResponseEntity.ok(tasksService.listTaskLists(UserContextHelper.currentUsername()));
    }

    @PostMapping("/lists")
    public ResponseEntity<GoogleTaskListDto> createTaskList(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(tasksService.createTaskList(UserContextHelper.currentUsername(), body.get("title")));
    }

    @PutMapping("/lists/{id}")
    public ResponseEntity<GoogleTaskListDto> updateTaskList(@PathVariable String id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(tasksService.updateTaskList(UserContextHelper.currentUsername(), id, body.get("title")));
    }

    @DeleteMapping("/lists/{id}")
    public ResponseEntity<Void> deleteTaskList(@PathVariable String id) {
        tasksService.deleteTaskList(UserContextHelper.currentUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/lists/{listId}/tasks")
    public ResponseEntity<List<GoogleTaskDto>> listTasks(@PathVariable String listId) {
        return ResponseEntity.ok(tasksService.listTasks(UserContextHelper.currentUsername(), listId));
    }

    @PostMapping("/lists/{listId}/tasks")
    public ResponseEntity<GoogleTaskDto> createTask(@PathVariable String listId, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(tasksService.createTask(UserContextHelper.currentUsername(), listId,
                body.get("title"), body.get("notes"), body.get("due")));
    }

    @PutMapping("/lists/{listId}/tasks/{taskId}")
    public ResponseEntity<GoogleTaskDto> updateTask(@PathVariable String listId, @PathVariable String taskId,
                                                     @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(tasksService.updateTask(UserContextHelper.currentUsername(), listId, taskId,
                body.get("title"), body.get("notes"), body.get("due")));
    }

    @PostMapping("/lists/{listId}/tasks/{taskId}/complete")
    public ResponseEntity<GoogleTaskDto> completeTask(@PathVariable String listId, @PathVariable String taskId) {
        return ResponseEntity.ok(tasksService.completeTask(UserContextHelper.currentUsername(), listId, taskId));
    }

    @PostMapping("/lists/{listId}/tasks/{taskId}/reopen")
    public ResponseEntity<GoogleTaskDto> reopenTask(@PathVariable String listId, @PathVariable String taskId) {
        return ResponseEntity.ok(tasksService.reopenTask(UserContextHelper.currentUsername(), listId, taskId));
    }

    @DeleteMapping("/lists/{listId}/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable String listId, @PathVariable String taskId) {
        tasksService.deleteTask(UserContextHelper.currentUsername(), listId, taskId);
        return ResponseEntity.noContent().build();
    }
}
