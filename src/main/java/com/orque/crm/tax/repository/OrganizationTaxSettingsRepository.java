package com.orque.crm.tax.repository;

import com.orque.crm.tax.entity.OrganizationTaxSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizationTaxSettingsRepository extends JpaRepository<OrganizationTaxSettings, Long> {
    Optional<OrganizationTaxSettings> findByOrganizationId(String organizationId);
}
