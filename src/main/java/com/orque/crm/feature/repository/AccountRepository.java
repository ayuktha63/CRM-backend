package com.orque.crm.feature.repository;

import com.orque.crm.feature.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    java.util.Optional<Account> findByCompanyNameIgnoreCase(String companyName);

    java.util.List<Account> findByOwner(String owner);

    java.util.List<Account> findByOrganizationId(String organizationId);
    java.util.List<Account> findByOrganizationIdAndOwner(String organizationId, String owner);
}
