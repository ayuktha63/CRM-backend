package com.orque.crm.google.tasks;

import com.orque.crm.common.UserContextHelper;
import com.orque.crm.google.tasks.dto.GoogleTaskDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Fallback task list used while Google isn't connected — everything here is stored in the CRM's
 * own database, scoped to the current user, until {@code /sync} pushes it into the real account.
 */
@RestController
@RequestMapping("/api/v1/google/tasks/local")
@RequiredArgsConstructor
public class LocalTaskController {

    private final LocalTaskService localTaskService;

    @GetMapping
    public ResponseEntity<List<GoogleTaskDto>> list() {
        return ResponseEntity.ok(localTaskService.listUnsynced(UserContextHelper.currentUsername()));
    }

    @PostMapping
    public ResponseEntity<GoogleTaskDto> create(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(localTaskService.create(UserContextHelper.currentUsername(),
                body.get("title"), body.get("notes"), body.get("due")));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoogleTaskDto> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(localTaskService.update(UserContextHelper.currentUsername(), id,
                body.get("title"), body.get("notes"), body.get("due")));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<GoogleTaskDto> complete(@PathVariable Long id) {
        return ResponseEntity.ok(localTaskService.setStatus(UserContextHelper.currentUsername(), id, "completed"));
    }

    @PostMapping("/{id}/reopen")
    public ResponseEntity<GoogleTaskDto> reopen(@PathVariable Long id) {
        return ResponseEntity.ok(localTaskService.setStatus(UserContextHelper.currentUsername(), id, "needsAction"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        localTaskService.delete(UserContextHelper.currentUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unsynced-count")
    public ResponseEntity<Map<String, Long>> unsyncedCount() {
        return ResponseEntity.ok(Map.of("count", localTaskService.countUnsynced(UserContextHelper.currentUsername())));
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Integer>> sync() {
        return ResponseEntity.ok(Map.of("synced", localTaskService.syncToGoogle(UserContextHelper.currentUsername())));
    }
}
