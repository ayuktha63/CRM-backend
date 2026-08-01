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

    /** Bulk fetch for merging custom-field columns onto a module's list view — one query
     *  for every row on the page instead of one round trip per row. */
    List<CrmCustomFieldData> findByModuleNameIgnoreCaseAndOrganizationIdAndRecordIdIn(
            String moduleName, String organizationId, List<Long> recordIds);

    void deleteByModuleNameIgnoreCaseAndOrganizationIdAndRecordId(
            String moduleName, String organizationId, Long recordId);

    /** Cleans up every saved value for a field that's being deleted, so they don't
     *  reappear as orphaned data if a field with the same name is re-added later. */
    void deleteByModuleNameIgnoreCaseAndFieldNameIgnoreCase(String moduleName, String fieldName);

    /** Every saved value under a field's current name, across all records — used to
     *  rename them in place when the field itself is renamed. */
    List<CrmCustomFieldData> findByModuleNameIgnoreCaseAndFieldNameIgnoreCase(String moduleName, String fieldName);
}
