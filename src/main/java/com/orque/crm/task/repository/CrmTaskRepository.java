
package com.orque.crm.task.repository;

import com.orque.crm.enums.TaskPriority;
import com.orque.crm.enums.TaskStatus;
import com.orque.crm.task.entity.CrmTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CrmTaskRepository extends JpaRepository<CrmTask, Long> {

    List<CrmTask> findByStatus(TaskStatus status);

    List<CrmTask> findByPriority(TaskPriority priority);

    List<CrmTask> findByAssignedToContainingIgnoreCase(String assignedTo);

    List<CrmTask> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String title,
            String description
    );

    List<CrmTask> findByDueDateBeforeAndStatusNot(
            LocalDateTime dateTime,
            TaskStatus status
    );
}