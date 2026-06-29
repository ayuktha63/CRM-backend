package com.orque.crm.timeline.repository;

import com.orque.crm.timeline.entity.TimelineEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TimelineRepository extends JpaRepository<TimelineEntry, Long> {
    List<TimelineEntry> findByModuleNameAndRecordIdOrderByCreatedAtDesc(String moduleName, Long recordId);
}
