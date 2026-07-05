package com.orque.crm.report.repository;

import com.orque.crm.report.entity.CrmDashboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CrmDashboardRepository extends JpaRepository<CrmDashboard, Long> {
    /**
     * A user should see: their own PRIVATE dashboards, any TEAM/PUBLIC dashboard within
     * their own organization (never another tenant's), and the same for the platform-owner
     * case where organizationId is null.
     */
    List<CrmDashboard> findByOrganizationIdAndCreatedByIgnoreCase(String organizationId, String createdBy);

    List<CrmDashboard> findByOrganizationIdAndShareTypeIgnoreCaseIn(String organizationId, List<String> shareTypes);

    List<CrmDashboard> findByCreatedByIgnoreCase(String createdBy);

    List<CrmDashboard> findByShareTypeIgnoreCaseIn(List<String> shareTypes);
}
