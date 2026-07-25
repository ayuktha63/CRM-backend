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

    /**
     * Current user's own ORG-WIDE license status (convenience) — deliberately always
     * the tenant's license, never a personal seat. authGuard's tenant-wide license-pending
     * gate depends on this being org-scoped for every caller; see getMySeat() below for
     * the per-user seat view instead. Do not repoint this at getMyStatus() again — that
     * regression broke the tenant gate for every non-admin (a personal seat's status is
     * not a substitute for the org's own license state).
     */
    @GetMapping("/status/me")
    public ResponseEntity<LicenseStatusResponse> getMyStatus(
            @RequestHeader(value = "X-Organization-Id", required = false) String headerOrgId) {
        String orgId = resolveOrgId(headerOrgId);
        if (orgId == null) {
            return ResponseEntity.ok(LicenseStatusResponse.builder()
                    .organizationName("SYSTEM_ADMIN — no org")
                    .build());
        }
        return ResponseEntity.ok(licenseService.getStatus(orgId));
    }

    /**
     * Current user's own individual seat (sub-license) if one was personally issued to
     * them, falling back to the tenant-wide license only for SYSTEM_ADMIN or when no
     * personal seat exists. Used by the Settings "License Information" screen — never
     * by authGuard's tenant-wide gate (see status/me above).
     */
    @GetMapping("/my-seat")
    public ResponseEntity<LicenseStatusResponse> getMySeat(
            @RequestHeader(value = "X-Organization-Id", required = false) String headerOrgId) {
        String orgId = resolveOrgId(headerOrgId);
        if (orgId == null) {
            return ResponseEntity.ok(LicenseStatusResponse.builder()
                    .organizationName("SYSTEM_ADMIN — no org")
                    .build());
        }
        return ResponseEntity.ok(licenseService.getMyStatus(orgId));
    }

    private String resolveOrgId(String headerOrgId) {
        String orgId = UserContextHelper.currentOrganizationId();
        // Fall back to the header sent by the frontend interceptor when the user's
        // DB record doesn't yet have organization_id populated (e.g. pre-SSO login).
        if (orgId == null || orgId.isBlank()) {
            orgId = headerOrgId;
        }
        return (orgId == null || orgId.isBlank()) ? null : orgId;
    }

    /** SYSTEM_ADMIN: generate a new encrypted license key string for testing / distribution. */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateKey(
            @Valid @RequestBody LicenseGenerateRequest request) {
        String key = licenseService.generateKey(request);
        return ResponseEntity.ok(Map.of("licenseKey", key));
    }
}
