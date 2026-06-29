package com.orque.crm.report.repository;

import com.orque.crm.report.entity.CrmReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CrmReportRepository extends JpaRepository<CrmReport, Long> {
    List<CrmReport> findByModuleNameIgnoreCase(String moduleName);
    List<CrmReport> findByShareTypeIgnoreCaseOrCreatedByIgnoreCase(String shareType, String createdBy);
}
