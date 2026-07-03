package com.orque.crm.config;

import com.orque.crm.auth.repository.UserRepository;
import com.orque.crm.enums.OrganizationStatus;
import com.orque.crm.organization.entity.Organization;
import com.orque.crm.organization.repository.OrganizationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;


/**
 * Runs once at startup.
 * 1. Creates a DEFAULT organization if none exists.
 * 2. Assigns all users with a null organizationId to the DEFAULT org.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializationService implements ApplicationRunner {

    private static final String DEFAULT_ORG_CODE = "DEFAULT";
    private static final String DEFAULT_ORG_NAME = "Default Organization";

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Organization defaultOrg = ensureDefaultOrganization();
        assignOrphanUsersToDefaultOrg(defaultOrg.getId());
    }

    private Organization ensureDefaultOrganization() {
        return organizationRepository.findByOrganizationCode(DEFAULT_ORG_CODE)
                .orElseGet(() -> {
                    Organization org = new Organization();
                    org.setOrganizationCode(DEFAULT_ORG_CODE);
                    org.setOrganizationName(DEFAULT_ORG_NAME);
                    org.setStatus(OrganizationStatus.ACTIVE);
                    Organization saved = organizationRepository.save(org);
                    log.info("Created default organization: id={}", saved.getId());
                    return saved;
                });
    }

    private void assignOrphanUsersToDefaultOrg(String defaultOrgId) {
        var orphans = userRepository.findByOrganizationIdIsNull();
        if (!orphans.isEmpty()) {
            orphans.forEach(u -> u.setOrganizationId(defaultOrgId));
            userRepository.saveAll(orphans);
            log.info("Assigned {} orphan user(s) to default org {}", orphans.size(), defaultOrgId);
        }
    }
}
