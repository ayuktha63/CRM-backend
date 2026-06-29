package com.orque.crm.settings.metadata.repository;

import com.orque.crm.settings.metadata.entity.CrmMetadataModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CrmMetadataModuleRepository extends JpaRepository<CrmMetadataModule, Long> {
    Optional<CrmMetadataModule> findByNameIgnoreCase(String name);
}
