package com.orque.crm.task.repository;

import com.orque.crm.task.entity.CrmCalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CrmCalendarEventRepository extends JpaRepository<CrmCalendarEvent, Long> {
    List<CrmCalendarEvent> findByCreatedByIgnoreCase(String createdBy);
}
