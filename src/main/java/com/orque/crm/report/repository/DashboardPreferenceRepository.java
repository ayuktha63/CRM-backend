package com.orque.crm.report.repository;

import com.orque.crm.report.entity.DashboardPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DashboardPreferenceRepository
        extends JpaRepository<DashboardPreference, Long> {

    List<DashboardPreference> findByUserIdOrderByDisplayOrder(Long userId);

    void deleteByUserId(Long userId);
}