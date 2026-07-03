package com.orque.crm.organization.service;

import com.orque.crm.auth.repository.UserRepository;
import com.orque.crm.common.UserContextHelper;
import com.orque.crm.enums.OrganizationStatus;
import com.orque.crm.license.repository.CrmLicenseRepository;
import com.orque.crm.organization.dto.OrganizationRequest;
import com.orque.crm.organization.dto.OrganizationResponse;
import com.orque.crm.organization.entity.Organization;
import com.orque.crm.organization.repository.OrganizationRepository;
import com.orque.crm.session.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository orgRepository;
    private final UserRepository userRepository;
    private final CrmLicenseRepository licenseRepository;
    private final UserSessionRepository sessionRepository;

    @Override
    public OrganizationResponse create(OrganizationRequest request) {
        if (!UserContextHelper.isSystemAdmin()) {
            throw new RuntimeException("Only SYSTEM_ADMIN can create organizations.");
        }
        if (orgRepository.existsByOrganizationCode(request.getOrganizationCode())) {
            throw new IllegalArgumentException("Organization code already exists: " + request.getOrganizationCode());
        }

        Organization org = Organization.builder()
                .organizationCode(request.getOrganizationCode().toUpperCase())
                .organizationName(request.getOrganizationName())
                .legalName(request.getLegalName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .country(request.getCountry())
                .timezone(request.getTimezone())
                .currency(request.getCurrency())
                .status(OrganizationStatus.ACTIVE)
                .build();

        org = orgRepository.save(org);
        log.info("Organization created: {} ({})", org.getOrganizationCode(), org.getId());
        return mapToResponse(org, true);
    }

    @Override
    public List<OrganizationResponse> listAll() {
        // Always scope to own org — no cross-tenant actor exists in CRM
        String myOrg = UserContextHelper.currentOrganizationId();
        List<Organization> orgs = (myOrg != null)
                ? orgRepository.findById(myOrg).map(List::of).orElse(List.of())
                : orgRepository.findAll();

        return orgs.stream().map(o -> mapToResponse(o, false)).toList();
    }

    @Override
    public OrganizationResponse getById(String id) {
        Organization org = orgRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Organization not found: " + id));

        if (!UserContextHelper.isSystemAdmin()) {
            if (!id.equals(UserContextHelper.currentOrganizationId())) {
                throw new RuntimeException("Access denied.");
            }
        }
        return mapToResponse(org, true);
    }

    @Override
    public OrganizationResponse update(String id, OrganizationRequest request) {
        if (!UserContextHelper.isSystemAdmin()) throw new RuntimeException("Only SYSTEM_ADMIN can update organizations.");

        Organization org = orgRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Organization not found: " + id));

        org.setOrganizationName(request.getOrganizationName());
        org.setLegalName(request.getLegalName());
        org.setEmail(request.getEmail());
        org.setPhone(request.getPhone());
        org.setAddress(request.getAddress());
        org.setCountry(request.getCountry());
        org.setTimezone(request.getTimezone());
        org.setCurrency(request.getCurrency());

        return mapToResponse(orgRepository.save(org), true);
    }

    @Override
    public void suspend(String id) {
        if (!UserContextHelper.isSystemAdmin()) throw new RuntimeException("Only SYSTEM_ADMIN can suspend organizations.");
        Organization org = orgRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Organization not found: " + id));
        org.setStatus(OrganizationStatus.SUSPENDED);
        orgRepository.save(org);
        log.info("Organization suspended: {}", id);
    }

    @Override
    public void activate(String id) {
        if (!UserContextHelper.isSystemAdmin()) throw new RuntimeException("Only SYSTEM_ADMIN can activate organizations.");
        Organization org = orgRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Organization not found: " + id));
        org.setStatus(OrganizationStatus.ACTIVE);
        orgRepository.save(org);
        log.info("Organization activated: {}", id);
    }

    // ──────────────────────────────────────────────────────────────────────────

    private OrganizationResponse mapToResponse(Organization org, boolean includeLicense) {
        long userCount = userRepository.countByOrganizationId(org.getId());

        OrganizationResponse.LicenseSummary licenseSummary = null;
        if (includeLicense) {
            licenseSummary = licenseRepository.findByOrganizationId(org.getId())
                    .map(lic -> {
                        LocalDate today = LocalDate.now();
                        long days = ChronoUnit.DAYS.between(today, lic.getEndDate());
                        return OrganizationResponse.LicenseSummary.builder()
                                .status(lic.getStatus().name())
                                .endDate(lic.getEndDate())
                                .daysRemaining((int) days)
                                .maxUsers(lic.getMaximumUsers())
                                .concurrentUsers(lic.getConcurrentUsers())
                                .build();
                    }).orElse(null);
        }

        return OrganizationResponse.builder()
                .id(org.getId())
                .organizationCode(org.getOrganizationCode())
                .organizationName(org.getOrganizationName())
                .legalName(org.getLegalName())
                .email(org.getEmail())
                .phone(org.getPhone())
                .address(org.getAddress())
                .country(org.getCountry())
                .timezone(org.getTimezone())
                .currency(org.getCurrency())
                .status(org.getStatus())
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .userCount(userCount)
                .license(licenseSummary)
                .build();
    }
}
