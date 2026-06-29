package com.orque.crm.settings.metadata.repository;

import com.orque.crm.settings.metadata.entity.CrmCustomModuleRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CrmCustomModuleRecordRepository extends JpaRepository<CrmCustomModuleRecord, Long> {
    List<CrmCustomModuleRecord> findByModuleNameIgnoreCase(String moduleName);
}
