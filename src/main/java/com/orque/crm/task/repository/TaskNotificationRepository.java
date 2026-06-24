package com.orque.crm.task.repository;

import com.orque.crm.enums.NotificationStatus;
import com.orque.crm.task.entity.TaskNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskNotificationRepository
        extends JpaRepository<TaskNotification, Long> {

    List<TaskNotification> findByTaskId(Long taskId);

    List<TaskNotification> findByStatus(NotificationStatus status);
}