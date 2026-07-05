package com.orque.crm.task.repository;

import com.orque.crm.task.entity.CrmCalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CrmCalendarEventRepository extends JpaRepository<CrmCalendarEvent, Long> {
    List<CrmCalendarEvent> findByCreatedByIgnoreCase(String createdBy);
    List<CrmCalendarEvent> findByOrganizationIdAndCreatedByIgnoreCase(String organizationId, String createdBy);
    List<CrmCalendarEvent> findByOrganizationId(String organizationId);
    Optional<CrmCalendarEvent> findBySyncId(String syncId);
}
