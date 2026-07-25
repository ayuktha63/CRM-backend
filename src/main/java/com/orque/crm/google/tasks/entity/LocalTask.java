package com.orque.crm.google.tasks.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Fallback storage used only while the user's Google account isn't connected (or lacks the
 * Tasks scope). Once connected, unsynced rows here can be pushed into the user's real Google
 * Tasks list via {@code POST /api/v1/google/tasks/local/sync} — after that this row is marked
 * synced and the live Google view is the source of truth for it going forward.
 */
@Entity
@Table(name = "local_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocalTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private String due; // ISO date, e.g. 2026-08-01

    @Column(nullable = false)
    private String status; // "needsAction" | "completed"

    private String completedAt;

    private String googleTaskId;

    @Column(nullable = false)
    private boolean synced;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "needsAction";
    }
}
