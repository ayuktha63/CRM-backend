package com.orque.crm.task.entity;

import com.orque.crm.enums.NotificationStatus;
import com.orque.crm.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long taskId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    private String recipient;

    @Column(columnDefinition = "TEXT")
    private String message;

    private LocalDateTime scheduledAt;

    private LocalDateTime sentAt;

    private LocalDateTime createdAt;
}