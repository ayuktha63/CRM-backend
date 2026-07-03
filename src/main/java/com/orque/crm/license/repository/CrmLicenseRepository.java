package com.orque.crm.license.repository;

import com.orque.crm.license.entity.CrmLicense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CrmLicenseRepository extends JpaRepository<CrmLicense, Long> {

    Optional<CrmLicense> findByOrganizationId(String organizationId);

    boolean existsByOrganizationId(String organizationId);
}
