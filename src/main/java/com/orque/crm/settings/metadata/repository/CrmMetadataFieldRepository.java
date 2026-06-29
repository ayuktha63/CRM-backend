package com.orque.crm.settings.metadata.repository;

import com.orque.crm.settings.metadata.entity.CrmMetadataField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CrmMetadataFieldRepository extends JpaRepository<CrmMetadataField, Long> {
    List<CrmMetadataField> findByModuleNameIgnoreCase(String moduleName);
}
