package com.orque.crm.feature.repository;

import com.orque.crm.feature.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findByRelatedTypeIgnoreCaseAndRelatedId(String relatedType, Long relatedId);
    List<Activity> findByRelatedTypeIgnoreCase(String relatedType);
    List<Activity> findByContactIgnoreCase(String contact);
    List<Activity> findByContactContainingIgnoreCase(String contact);
    List<Activity> findByOrganizationId(String organizationId);
    List<Activity> findByOrganizationIdAndAssignedTo(String organizationId, String assignedTo);
}
