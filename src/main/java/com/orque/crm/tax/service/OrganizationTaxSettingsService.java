package com.orque.crm.tax.service;

import com.orque.crm.common.UserContextHelper;
import com.orque.crm.tax.dto.TaxSettingsRequest;
import com.orque.crm.tax.dto.TaxSettingsResponse;
import com.orque.crm.tax.entity.OrganizationTaxSettings;
import com.orque.crm.tax.repository.OrganizationTaxSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Self-service tax configuration — always resolves to the caller's own organizationId
 * (never accepts one from the client) and requires SYSTEM_ADMIN for writes, matching
 * the same secure pattern already used for the billing profile endpoints.
 */
@Service
@RequiredArgsConstructor
public class OrganizationTaxSettingsService {

    private final OrganizationTaxSettingsRepository repository;

    public TaxSettingsResponse getMySettings() {
        String orgId = UserContextHelper.currentOrganizationId();
        if (orgId == null) {
            throw new RuntimeException("No organization context for current user.");
        }
        return repository.findByOrganizationId(orgId)
                .map(this::toResponse)
                .orElse(TaxSettingsResponse.builder().organizationId(orgId).enabled(false).build());
    }

    public TaxSettingsResponse updateMySettings(TaxSettingsRequest request) {
        if (!UserContextHelper.isSystemAdmin()) {
            throw new RuntimeException("Only the tenant's SYSTEM_ADMIN can update tax settings.");
        }
        String orgId = UserContextHelper.currentOrganizationId();
        if (orgId == null) {
            throw new RuntimeException("No organization context for current user.");
        }

        OrganizationTaxSettings settings = repository.findByOrganizationId(orgId)
                .orElseGet(() -> OrganizationTaxSettings.builder().organizationId(orgId).build());

        settings.setCountryCode(request.getCountryCode());
        settings.setCountryName(request.getCountryName());
        settings.setTaxSystem(request.getTaxSystem());
        settings.setRegistrationLabel(request.getRegistrationLabel());
        settings.setRegistrationNumber(request.getRegistrationNumber());
        settings.setBusinessState(request.getBusinessState());
        settings.setRequiresBusinessState(Boolean.TRUE.equals(request.getRequiresBusinessState()));
        settings.setConfigJson(request.getConfigJson());
        settings.setEnabled(request.getEnabled() == null || request.getEnabled());

        return toResponse(repository.save(settings));
    }

    /** Used internally by Quote/Invoice save flows — no access control, called server-side only. */
    public OrganizationTaxSettings findForOrg(String organizationId) {
        if (organizationId == null) return null;
        return repository.findByOrganizationId(organizationId).orElse(null);
    }

    private TaxSettingsResponse toResponse(OrganizationTaxSettings s) {
        return TaxSettingsResponse.builder()
                .organizationId(s.getOrganizationId())
                .countryCode(s.getCountryCode())
                .countryName(s.getCountryName())
                .taxSystem(s.getTaxSystem())
                .registrationLabel(s.getRegistrationLabel())
                .registrationNumber(s.getRegistrationNumber())
                .businessState(s.getBusinessState())
                .requiresBusinessState(s.getRequiresBusinessState())
                .configJson(s.getConfigJson())
                .enabled(s.getEnabled())
                .build();
    }
}
