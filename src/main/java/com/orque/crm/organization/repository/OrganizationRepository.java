package com.orque.crm.organization.repository;

import com.orque.crm.enums.OrganizationStatus;
import com.orque.crm.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, String> {

    Optional<Organization> findByOrganizationCode(String organizationCode);

    /** Case-insensitive lookup — organization_code should be treated as a stable key
     *  regardless of how it was cased when the org row was first created. */
    Optional<Organization> findByOrganizationCodeIgnoreCase(String organizationCode);

    boolean existsByOrganizationCode(String organizationCode);

    List<Organization> findAllByStatus(OrganizationStatus status);

    Optional<Organization> findFirstByOrderByCreatedAtAsc();
}
