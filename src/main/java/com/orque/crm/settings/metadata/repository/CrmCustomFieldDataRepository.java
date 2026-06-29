package com.orque.crm.settings.metadata.repository;

import com.orque.crm.settings.metadata.entity.CrmCustomFieldData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CrmCustomFieldDataRepository extends JpaRepository<CrmCustomFieldData, Long> {
    List<CrmCustomFieldData> findByModuleNameIgnoreCaseAndRecordId(String moduleName, Long recordId);
    Optional<CrmCustomFieldData> findByModuleNameIgnoreCaseAndRecordIdAndFieldNameIgnoreCase(String moduleName, Long recordId, String fieldName);
    void deleteByModuleNameIgnoreCaseAndRecordId(String moduleName, Long recordId);
}
