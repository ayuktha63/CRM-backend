package com.orque.crm.task.dto;

import com.orque.crm.enums.NotificationStatus;
import com.orque.crm.enums.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskNotificationResponse {

    private Long id;

    private Long taskId;

    private NotificationType notificationType;

    private NotificationStatus status;

    private String recipient;

    private String message;

    private LocalDateTime scheduledAt;

    private LocalDateTime sentAt;
}