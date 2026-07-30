package com.orque.crm.google.tasks;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.Task;
import com.google.api.services.tasks.model.TaskList;
import com.orque.crm.google.entity.GoogleWorkspaceCredential;
import com.orque.crm.google.tasks.dto.GoogleTaskDto;
import com.orque.crm.google.tasks.dto.GoogleTaskListDto;
import com.orque.crm.google.token.GoogleNotConnectedException;
import com.orque.crm.google.token.GoogleTokenManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Every call here goes straight to Google Tasks, live — no local mirror is read for display.
 * CRM tasks and Google Tasks are two independent lists surfaced side by side in the UI; this
 * service only talks to Google's copy.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleTasksService {

    private final GoogleTokenManager tokenManager;

    // ── Task lists ───────────────────────────────────────────────────────────

    public java.util.List<GoogleTaskListDto> listTaskLists(String username) {
        GoogleWorkspaceCredential credential = requireTasksConnected(username);
        try {
            Tasks client = buildClient(credential);
            var response = client.tasklists().list().execute();
            tokenManager.recordApiSuccess(username);
            if (response.getItems() == null) return java.util.List.of();
            return response.getItems().stream().map(this::toListDto).toList();
        } catch (Exception e) {
            log.warn("Google Tasks listTaskLists failed for user {}: {}", username, e.getMessage());
            tokenManager.recordIfRevoked(username, e);
            throw new IllegalStateException("Failed to fetch task lists", e);
        }
    }

    /** Used when pushing locally-created fallback tasks up to Google: reuses the first existing
     *  list rather than always creating a new one, since most accounts just have "My Tasks". */
    public String getOrCreateDefaultListId(String username) {
        java.util.List<GoogleTaskListDto> lists = listTaskLists(username);
        if (!lists.isEmpty()) return lists.get(0).getId();
        return createTaskList(username, "My Tasks").getId();
    }

    public GoogleTaskListDto createTaskList(String username, String title) {
        GoogleWorkspaceCredential credential = requireTasksConnected(username);
        try {
            Tasks client = buildClient(credential);
            TaskList created = client.tasklists().insert(new TaskList().setTitle(title)).execute();
            tokenManager.recordApiSuccess(username);
            return toListDto(created);
        } catch (Exception e) {
            log.warn("Google Tasks createTaskList failed for user {}: {}", username, e.getMessage());
            tokenManager.recordIfRevoked(username, e);
            throw new IllegalStateException("Failed to create task list", e);
        }
    }

    public GoogleTaskListDto updateTaskList(String username, String taskListId, String title) {
        GoogleWorkspaceCredential credential = requireTasksConnected(username);
        try {
            Tasks client = buildClient(credential);
            TaskList updated = client.tasklists().patch(taskListId, new TaskList().setTitle(title)).execute();
            tokenManager.recordApiSuccess(username);
            return toListDto(updated);
        } catch (Exception e) {
            log.warn("Google Tasks updateTaskList failed for user {}: {}", username, e.getMessage());
            tokenManager.recordIfRevoked(username, e);
            throw new IllegalStateException("Failed to update task list", e);
        }
    }

    public void deleteTaskList(String username, String taskListId) {
        GoogleWorkspaceCredential credential = requireTasksConnected(username);
        try {
            buildClient(credential).tasklists().delete(taskListId).execute();
            tokenManager.recordApiSuccess(username);
        } catch (Exception e) {
            log.warn("Google Tasks deleteTaskList failed for user {}: {}", username, e.getMessage());
            tokenManager.recordIfRevoked(username, e);
            throw new IllegalStateException("Failed to delete task list", e);
        }
    }

    // ── Tasks ────────────────────────────────────────────────────────────────

    public java.util.List<GoogleTaskDto> listTasks(String username, String taskListId) {
        GoogleWorkspaceCredential credential = requireTasksConnected(username);
        try {
            Tasks client = buildClient(credential);
            // Google hides a task from list() as soon as it's completed — showCompleted alone
            // doesn't bring it back, showHidden is also required, or completed tasks vanish
            // from CRM entirely instead of showing under Completed.
            var response = client.tasks().list(taskListId).setShowCompleted(true).setShowHidden(true).execute();
            tokenManager.recordApiSuccess(username);
            if (response.getItems() == null) return java.util.List.of();
            return response.getItems().stream().map(t -> toTaskDto(taskListId, t)).toList();
        } catch (Exception e) {
            log.warn("Google Tasks listTasks failed for user {}: {}", username, e.getMessage());
            tokenManager.recordIfRevoked(username, e);
            throw new IllegalStateException("Failed to fetch tasks", e);
        }
    }

    public GoogleTaskDto createTask(String username, String taskListId, String title, String notes, String due) {
        GoogleWorkspaceCredential credential = requireTasksConnected(username);
        try {
            Tasks client = buildClient(credential);
            Task task = new Task().setTitle(title).setNotes(notes);
            if (due != null && !due.isBlank()) task.setDue(toRfc3339(due));
            Task created = client.tasks().insert(taskListId, task).execute();
            tokenManager.recordApiSuccess(username);
            return toTaskDto(taskListId, created);
        } catch (Exception e) {
            log.warn("Google Tasks createTask failed for user {}: {}", username, e.getMessage());
            tokenManager.recordIfRevoked(username, e);
            throw new IllegalStateException("Failed to create task", e);
        }
    }

    public GoogleTaskDto updateTask(String username, String taskListId, String taskId, String title, String notes, String due) {
        GoogleWorkspaceCredential credential = requireTasksConnected(username);
        try {
            Tasks client = buildClient(credential);
            Task task = new Task().setTitle(title).setNotes(notes);
            if (due != null && !due.isBlank()) task.setDue(toRfc3339(due));
            Task updated = client.tasks().patch(taskListId, taskId, task).execute();
            tokenManager.recordApiSuccess(username);
            return toTaskDto(taskListId, updated);
        } catch (Exception e) {
            log.warn("Google Tasks updateTask failed for user {}: {}", username, e.getMessage());
            tokenManager.recordIfRevoked(username, e);
            throw new IllegalStateException("Failed to update task", e);
        }
    }

    public GoogleTaskDto completeTask(String username, String taskListId, String taskId) {
        return setStatus(username, taskListId, taskId, "completed");
    }

    public GoogleTaskDto reopenTask(String username, String taskListId, String taskId) {
        return setStatus(username, taskListId, taskId, "needsAction");
    }

    private GoogleTaskDto setStatus(String username, String taskListId, String taskId, String status) {
        GoogleWorkspaceCredential credential = requireTasksConnected(username);
        try {
            Tasks client = buildClient(credential);
            Task patch = new Task().setStatus(status);
            if ("completed".equals(status)) {
                patch.setCompleted(new com.google.api.client.util.DateTime(System.currentTimeMillis()).toStringRfc3339());
            } else {
                patch.setCompleted(null);
            }
            Task updated = client.tasks().patch(taskListId, taskId, patch).execute();
            tokenManager.recordApiSuccess(username);
            return toTaskDto(taskListId, updated);
        } catch (Exception e) {
            log.warn("Google Tasks status update failed for user {}: {}", username, e.getMessage());
            tokenManager.recordIfRevoked(username, e);
            throw new IllegalStateException("Failed to update task status", e);
        }
    }

    public void deleteTask(String username, String taskListId, String taskId) {
        GoogleWorkspaceCredential credential = requireTasksConnected(username);
        try {
            buildClient(credential).tasks().delete(taskListId, taskId).execute();
            tokenManager.recordApiSuccess(username);
        } catch (Exception e) {
            log.warn("Google Tasks deleteTask failed for user {}: {}", username, e.getMessage());
            tokenManager.recordIfRevoked(username, e);
            throw new IllegalStateException("Failed to delete task", e);
        }
    }

    // ── Internals ──────────────────────────────────────────────────────────

    /** Accounts connected before the Tasks scope was added to the OAuth request only hold
     *  gmail/calendar grants — Google won't silently expand that, so calling the Tasks API with
     *  such a credential 403s. Surface it as the same "reconnect" signal the frontend already
     *  understands (409) instead of a bare 500 that gets swallowed as an empty task list. */
    private GoogleWorkspaceCredential requireTasksConnected(String username) {
        GoogleWorkspaceCredential credential = tokenManager.requireConnected(username);
        if (!credential.hasScope("tasks")) {
            throw new GoogleNotConnectedException(
                    "Google Tasks access wasn't granted for this account — disconnect and reconnect Google to grant it.");
        }
        return credential;
    }

    private Tasks buildClient(GoogleWorkspaceCredential credential) throws Exception {
        return new Tasks.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(),
                tokenManager.buildCredential(credential))
                .setApplicationName("Orque CRM")
                .build();
    }

    private String toRfc3339(String isoDate) {
        return isoDate + "T00:00:00.000Z";
    }

    private GoogleTaskListDto toListDto(TaskList list) {
        return GoogleTaskListDto.builder().id(list.getId()).title(list.getTitle()).build();
    }

    private GoogleTaskDto toTaskDto(String taskListId, Task task) {
        return GoogleTaskDto.builder()
                .id(task.getId())
                .taskListId(taskListId)
                .title(task.getTitle())
                .notes(task.getNotes())
                .due(task.getDue())
                .status(task.getStatus())
                .completedAt(task.getCompleted())
                .build();
    }
}
