package com.orque.crm.report.repository;

import com.orque.crm.report.entity.CrmDashboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CrmDashboardRepository extends JpaRepository<CrmDashboard, Long> {
    List<CrmDashboard> findByShareTypeIgnoreCaseOrCreatedByIgnoreCase(String shareType, String createdBy);
}
