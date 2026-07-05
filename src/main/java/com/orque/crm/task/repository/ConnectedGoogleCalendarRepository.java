package com.orque.crm.task.repository;

import com.orque.crm.task.entity.ConnectedGoogleCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectedGoogleCalendarRepository extends JpaRepository<ConnectedGoogleCalendar, Long> {
    Optional<ConnectedGoogleCalendar> findByOwnerIgnoreCase(String owner);
    List<ConnectedGoogleCalendar> findBySyncEnabledTrue();
}
