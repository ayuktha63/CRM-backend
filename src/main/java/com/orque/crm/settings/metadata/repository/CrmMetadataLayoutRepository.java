package com.orque.crm.settings.metadata.repository;

import com.orque.crm.settings.metadata.entity.CrmMetadataLayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CrmMetadataLayoutRepository extends JpaRepository<CrmMetadataLayout, Long> {
    Optional<CrmMetadataLayout> findByModuleNameIgnoreCaseAndRoleNameIgnoreCase(String moduleName, String roleName);
}
