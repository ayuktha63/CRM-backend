package com.orque.crm.license.controller;

import com.orque.crm.common.UserContextHelper;
import com.orque.crm.license.dto.LicenseActivationRequest;
import com.orque.crm.license.dto.LicenseGenerateRequest;
import com.orque.crm.license.dto.LicenseStatusResponse;
import com.orque.crm.license.service.LicenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/license")
@RequiredArgsConstructor
public class LicenseController {

    private final LicenseService licenseService;

    /** SYSTEM_ADMIN: activate or replace a license for an organization. */
    @PostMapping("/activate")
    public ResponseEntity<LicenseStatusResponse> activate(
            @Valid @RequestBody LicenseActivationRequest request) {
        return ResponseEntity.ok(licenseService.activate(request));
    }

    /** Org admin or SYSTEM_ADMIN: get license status for an organization. */
    @GetMapping("/status/{organizationId}")
    public ResponseEntity<LicenseStatusResponse> getStatus(
            @PathVariable String organizationId) {
        return ResponseEntity.ok(licenseService.getStatus(organizationId));
    }

    /** Current user's own org license status (convenience). */
    @GetMapping("/status/me")
    public ResponseEntity<LicenseStatusResponse> getMyStatus() {
        String orgId = UserContextHelper.currentOrganizationId();
        if (orgId == null) {
            return ResponseEntity.ok(LicenseStatusResponse.builder()
                    .organizationName("SYSTEM_ADMIN — no org")
                    .build());
        }
        return ResponseEntity.ok(licenseService.getStatus(orgId));
    }

    /** SYSTEM_ADMIN: generate a new encrypted license key string for testing / distribution. */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateKey(
            @Valid @RequestBody LicenseGenerateRequest request) {
        String key = licenseService.generateKey(request);
        return ResponseEntity.ok(Map.of("licenseKey", key));
    }
}
