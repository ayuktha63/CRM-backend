package com.orque.crm.google.tasks;

import com.orque.crm.google.tasks.dto.GoogleTaskDto;
import com.orque.crm.google.tasks.entity.LocalTask;
import com.orque.crm.google.tasks.repository.LocalTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Fallback CRUD for tasks created while Google isn't connected. Kept deliberately shaped like
 * {@link GoogleTaskDto} (via {@code "local-" + id}) so the tasks-workspace frontend can render
 * both sources through one model without a mode-specific UI.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalTaskService {

    private final LocalTaskRepository repository;
    private final GoogleTasksService googleTasksService;

    public List<GoogleTaskDto> listUnsynced(String owner) {
        return repository.findByOwnerIgnoreCaseAndSyncedFalseOrderByCreatedAtDesc(owner)
                .stream().map(this::toDto).toList();
    }

    public GoogleTaskDto create(String owner, String title, String notes, String due) {
        LocalTask task = LocalTask.builder()
                .owner(owner).title(title).notes(notes).due(due)
                .status("needsAction").synced(false)
                .build();
        return toDto(repository.save(task));
    }

    public GoogleTaskDto update(String owner, Long id, String title, String notes, String due) {
        LocalTask task = requireOwned(owner, id);
        task.setTitle(title);
        task.setNotes(notes);
        task.setDue(due);
        return toDto(repository.save(task));
    }

    public GoogleTaskDto setStatus(String owner, Long id, String status) {
        LocalTask task = requireOwned(owner, id);
        task.setStatus(status);
        task.setCompletedAt("completed".equals(status)
                ? java.time.Instant.now().toString() : null);
        return toDto(repository.save(task));
    }

    public void delete(String owner, Long id) {
        LocalTask task = requireOwned(owner, id);
        repository.delete(task);
    }

    /** Pushes every unsynced local task into the user's real Google Tasks account. Returns how
     *  many were synced; each row is marked synced with its new Google id right after, so a
     *  partial failure part-way through doesn't re-push what already succeeded on retry. */
    @Transactional
    public int syncToGoogle(String owner) {
        List<LocalTask> unsynced = repository.findByOwnerIgnoreCaseAndSyncedFalse(owner);
        if (unsynced.isEmpty()) return 0;

        String listId = googleTasksService.getOrCreateDefaultListId(owner);
        int count = 0;
        for (LocalTask local : unsynced) {
            try {
                GoogleTaskDto created = googleTasksService.createTask(owner, listId, local.getTitle(), local.getNotes(), local.getDue());
                if ("completed".equals(local.getStatus())) {
                    googleTasksService.completeTask(owner, listId, created.getId());
                }
                local.setSynced(true);
                local.setGoogleTaskId(created.getId());
                repository.save(local);
                count++;
            } catch (Exception e) {
                log.warn("Failed to sync local task {} to Google for user {}: {}", local.getId(), owner, e.getMessage());
            }
        }
        return count;
    }

    public long countUnsynced(String owner) {
        return repository.findByOwnerIgnoreCaseAndSyncedFalse(owner).size();
    }

    private LocalTask requireOwned(String owner, Long id) {
        LocalTask task = repository.findById(id).orElseThrow(() -> new NoSuchElementException("Task not found"));
        if (!task.getOwner().equalsIgnoreCase(owner)) {
            throw new NoSuchElementException("Task not found");
        }
        return task;
    }

    private GoogleTaskDto toDto(LocalTask task) {
        return GoogleTaskDto.builder()
                .id("local-" + task.getId())
                .taskListId("local")
                .title(task.getTitle())
                .notes(task.getNotes())
                .due(task.getDue())
                .status(task.getStatus())
                .completedAt(task.getCompletedAt())
                .build();
    }
}
