package com.orque.crm.feature.controller;

import com.orque.crm.license.entity.CrmLicense;
import com.orque.crm.license.repository.CrmLicenseRepository;
import com.orque.crm.license.util.LicenseEncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/settings")
@CrossOrigin
@RequiredArgsConstructor
public class SystemSettingsController {

    private final CrmLicenseRepository licenseRepository;
    private final LicenseEncryptionUtil encryptionUtil;

    @GetMapping("/license")
    public ResponseEntity<Map<String, Object>> getLicenseInfo() {
        String orgId = com.orque.crm.common.UserContextHelper.currentOrganizationId();
        if (orgId == null || orgId.isBlank()) {
            orgId = "SYSTEM";
        }

        CrmLicense license = licenseRepository.findByOrganizationId(orgId).orElse(null);
        if (license == null) {
            return ResponseEntity.ok(Map.of(
                    "productName", "CRM",
                    "startDate", "",
                    "endDate", "",
                    "gracePeriodDays", 0,
                    "concurrentUsers", 0,
                    "configured", false
            ));
        }

        try {
            LicenseEncryptionUtil.OpacLicensePayload payload = encryptionUtil.decryptHex(license.getLicenseKey());
            LicenseEncryptionUtil.OpacProduct crmProduct = payload.getProducts().stream()
                    .filter(p -> "CRM".equalsIgnoreCase(p.getProductName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("CRM configuration not found"));

            return ResponseEntity.ok(Map.of(
                    "productName", "CRM",
                    "tenantName", payload.getTenant() != null ? payload.getTenant().getCompanyName() : "",
                    "startDate", crmProduct.getStartDate(),
                    "endDate", crmProduct.getEndDate(),
                    "gracePeriodDays", crmProduct.getGracePeriod(),
                    "concurrentUsers", crmProduct.getConcurrentLimit(),
                    "features", crmProduct.getFeatures(),
                    "configured", true
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "productName", "CRM",
                    "startDate", "",
                    "endDate", "",
                    "gracePeriodDays", 0,
                    "concurrentUsers", 0,
                    "configured", false,
                    "error", "Failed to decrypt saved license: " + e.getMessage()
            ));
        }
    }
}
