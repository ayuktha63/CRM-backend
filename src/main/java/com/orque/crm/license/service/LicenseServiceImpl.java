package com.orque.crm.license.service;

import com.orque.crm.common.UserContextHelper;
import com.orque.crm.enums.LicenseStatus;
import com.orque.crm.license.dto.LicenseActivationRequest;
import com.orque.crm.license.dto.LicenseGenerateRequest;
import com.orque.crm.license.dto.LicenseStatusResponse;
import com.orque.crm.license.entity.CrmLicense;
import com.orque.crm.license.repository.CrmLicenseRepository;
import com.orque.crm.license.util.LicenseEncryptionUtil;
import com.orque.crm.license.util.LicenseEncryptionUtil.OpacLicensePayload;
import com.orque.crm.organization.entity.Organization;
import com.orque.crm.organization.repository.OrganizationRepository;
import com.orque.crm.auth.repository.UserRepository;
import com.orque.crm.session.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class LicenseServiceImpl implements LicenseService {

    @Value("${opac.base-url:http://localhost:8082}")
    private String opacBaseUrl;

    private final CrmLicenseRepository licenseRepository;
    private final OrganizationRepository orgRepository;
    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final LicenseEncryptionUtil encryptionUtil;

    @Override
    @Transactional
    public LicenseStatusResponse activate(LicenseActivationRequest request) {
        if (!UserContextHelper.isSystemAdmin() && !UserContextHelper.currentRole().name().equals("ADMIN")) {
            throw new RuntimeException("Only administrators can activate licenses.");
        }

        if (!UserContextHelper.isSystemAdmin()) {
            String myOrg = UserContextHelper.currentOrganizationId();
            if (!request.getOrganizationId().equals(myOrg)) {
                throw new RuntimeException("Access denied. You can only activate a license for your own organization.");
            }
        }

        LicenseEncryptionUtil.OpacLicensePayload payload = encryptionUtil.decryptHex(request.getLicenseKey());

        // Validate digital signature
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String productsStr = objectMapper.writeValueAsString(payload.getProducts());
            String calculatedSig = encryptionUtil.generateHmac(productsStr);
            if (!calculatedSig.equalsIgnoreCase(payload.getDigitalSignature())) {
                throw new IllegalArgumentException("License signature verification failed. The license file might have been tampered with.");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to verify digital signature: " + e.getMessage(), e);
        }

        // Find CRM product configuration
        LicenseEncryptionUtil.OpacProduct crmProduct = payload.getProducts().stream()
                .filter(p -> "CRM".equalsIgnoreCase(p.getProductName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("License does not contain configuration for product 'CRM'"));

        LocalDate startDate = LocalDate.parse(crmProduct.getStartDate());
        LocalDate endDate = LocalDate.parse(crmProduct.getEndDate());

        CrmLicense license;
        String orgName;

        if ("SYSTEM".equalsIgnoreCase(request.getOrganizationId())) {
            log.info("Activating system-wide license, product=CRM endDate={}", endDate);

            license = licenseRepository.findByOrganizationId("SYSTEM")
                    .orElse(CrmLicense.builder().organizationId("SYSTEM").build());

            orgName = "System";
        } else {
            Organization org = orgRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new NoSuchElementException("Organization not found: " + request.getOrganizationId()));

            log.info("Activating license for org={} orgCode={} endDate={}",
                    org.getOrganizationCode(), payload.getTenant().getTenantName(), endDate);

            license = licenseRepository.findByOrganizationId(request.getOrganizationId())
                    .orElse(CrmLicense.builder().organizationId(request.getOrganizationId()).build());

            orgName = org.getOrganizationName();
        }

        license.setLicenseKey(request.getLicenseKey());
        license.setLicenseHash(computeSha256(request.getLicenseKey()));
        license.setLicenseName(request.getLicenseName() != null && !request.getLicenseName().isBlank() ? request.getLicenseName() : ("SYSTEM".equalsIgnoreCase(request.getOrganizationId()) ? "CRM System License" : "CRM License"));
        license.setOrgCode(payload.getTenant() != null ? payload.getTenant().getTenantName() : "SYSTEM");
        license.setProductName("CRM");
        license.setStartDate(startDate);
        license.setEndDate(endDate);
        license.setGracePeriodDays(crmProduct.getGracePeriod());
        license.setMaximumUsers(crmProduct.getUserLimit());
        license.setConcurrentUsers(crmProduct.getConcurrentLimit());
        license.setStatus(computeStatus(endDate, crmProduct.getGracePeriod()));

        // Save features JSON list
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            license.setFeaturesJson(mapper.writeValueAsString(crmProduct.getFeatures()));
        } catch (Exception ex) {
            license.setFeaturesJson("[]");
        }

        licenseRepository.save(license);
        log.info("License activated for organization {}", license.getOrgCode());

        return buildStatusResponse(license, orgName);
    }

    private String computeSha256(String text) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException("SHA-256 hashing failed", ex);
        }
    }

    @Override
    public LicenseStatusResponse getStatus(String organizationId) {
        // Allow org admin to view their own license or system admin to view any
        if (!UserContextHelper.isSystemAdmin()) {
            String myOrg = UserContextHelper.currentOrganizationId();
            if (!organizationId.equals(myOrg)) {
                throw new RuntimeException("Access denied.");
            }
        }

        CrmLicense license = licenseRepository.findByOrganizationId(organizationId).orElse(null);

        // No local license — fall back to OPAC master license check
        if (license == null && !"SYSTEM".equalsIgnoreCase(organizationId)) {
            Organization org = orgRepository.findById(organizationId).orElse(null);
            if (org != null) {
                LicenseStatusResponse opacStatus = checkOpacMasterLicense(org.getOrganizationCode(), organizationId, org.getOrganizationName());
                if (opacStatus != null) return opacStatus;
            }
            return LicenseStatusResponse.builder()
                    .organizationId(organizationId)
                    .organizationName(org != null ? org.getOrganizationName() : null)
                    .status(LicenseStatus.EXPIRED)
                    .features(new java.util.ArrayList<>())
                    .build();
        }

        if (license == null) {
            return LicenseStatusResponse.builder()
                    .organizationId(organizationId)
                    .status(LicenseStatus.EXPIRED)
                    .features(new java.util.ArrayList<>())
                    .build();
        }

        String orgName = "SYSTEM".equalsIgnoreCase(organizationId) ? "System" :
                orgRepository.findById(organizationId)
                .map(Organization::getOrganizationName).orElse("Unknown");

        return buildStatusResponse(license, orgName);
    }

    @SuppressWarnings("unchecked")
    private LicenseStatusResponse checkOpacMasterLicense(String orgCode, String organizationId, String orgName) {
        try {
            org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
            org.springframework.http.ResponseEntity<Map> resp = rt.getForEntity(
                    opacBaseUrl + "/api/internal/crm-license/" + orgCode, Map.class);
            Map<String, Object> body = resp.getBody();
            if (body == null || !Boolean.TRUE.equals(body.get("active"))) return null;

            boolean inGrace = Boolean.TRUE.equals(body.get("inGrace"));
            int daysRemaining = body.get("daysRemaining") instanceof Number ? ((Number) body.get("daysRemaining")).intValue() : 0;
            int graceRemaining = body.get("graceRemaining") instanceof Number ? ((Number) body.get("graceRemaining")).intValue() : 0;
            int userLimit = body.get("userLimit") instanceof Number ? ((Number) body.get("userLimit")).intValue() : 0;
            int concurrentLimit = body.get("concurrentLimit") instanceof Number ? ((Number) body.get("concurrentLimit")).intValue() : 0;
            int gracePeriod = body.get("gracePeriod") instanceof Number ? ((Number) body.get("gracePeriod")).intValue() : 30;
            String expiry = body.getOrDefault("expiry", "").toString();
            List<String> features = body.get("features") instanceof List ? (List<String>) body.get("features") : new java.util.ArrayList<>();

            LocalDate endDate = expiry.isBlank() ? LocalDate.now().plusYears(1) : LocalDate.parse(expiry);
            LicenseStatus status = inGrace ? LicenseStatus.GRACE : LicenseStatus.ACTIVE;

            log.info("OPAC master license confirmed for org={} status={}", orgCode, status);

            return LicenseStatusResponse.builder()
                    .organizationId(organizationId)
                    .organizationName(orgName)
                    .licenseName("Master License (via OPAC)")
                    .orgCode(orgCode)
                    .endDate(endDate)
                    .gracePeriodDays(gracePeriod)
                    .maximumUsers(userLimit)
                    .concurrentUsers(concurrentLimit)
                    .status(status)
                    .daysRemaining(daysRemaining)
                    .inGracePeriod(inGrace)
                    .graceRemaining(graceRemaining)
                    .features(features)
                    .build();
        } catch (Exception e) {
            log.warn("Could not reach OPAC for license check (org={}): {}", orgCode, e.getMessage());
            return null;
        }
    }

    @Override
    public LicenseCheckResult check(String organizationId) {
        if (organizationId == null) {
            return new LicenseCheckResult(true, false, 0, "No org — system user", 0);
        }

        CrmLicense license = licenseRepository.findByOrganizationId(organizationId).orElse(null);

        if (license == null) {
            if ("SYSTEM".equalsIgnoreCase(organizationId)) {
                return new LicenseCheckResult(false, false, 0, "CRM license is not configured", 0);
            }
            // No license yet — restrict access
            return new LicenseCheckResult(false, false, 0, "No license configured", 0);
        }

        int concurrentLimit = license.getConcurrentUsers() != null ? license.getConcurrentUsers() : 0;
        LocalDate today = LocalDate.now();
        LocalDate startDate = license.getStartDate();
        LocalDate endDate = license.getEndDate();
        int graceDays = license.getGracePeriodDays() != null ? license.getGracePeriodDays() : 30;

        if (startDate != null && today.isBefore(startDate)) {
            return new LicenseCheckResult(false, false, 0, "License is not active yet (starts on " + startDate + ")", concurrentLimit);
        }

        if (!today.isAfter(endDate)) {
            // Active
            return new LicenseCheckResult(true, false, 0, "License active", concurrentLimit);
        }

        long daysOver = ChronoUnit.DAYS.between(endDate, today);
        if (daysOver <= graceDays) {
            int remaining = (int)(graceDays - daysOver);
            return new LicenseCheckResult(true, true, remaining,
                    "License in grace period — " + remaining + " day(s) remaining", concurrentLimit);
        }

        return new LicenseCheckResult(false, false, 0,
                "License expired " + daysOver + " day(s) ago. Please renew.", concurrentLimit);
    }

    @Override
    public String generateKey(LicenseGenerateRequest request) {
        if (!UserContextHelper.isSystemAdmin()) {
            throw new RuntimeException("Only SYSTEM_ADMIN can generate license keys.");
        }

        LicenseEncryptionUtil.OpacProduct product = LicenseEncryptionUtil.OpacProduct.builder()
                .productName(request.getProductName())
                .startDate(request.getStartDate().toString())
                .endDate(request.getEndDate().toString())
                .userLimit(request.getMaxUsers())
                .concurrentLimit(request.getConcurrentUsers())
                .gracePeriod(request.getGracePeriodDays())
                .features(java.util.Arrays.asList(
                    "/dashboard", "/leads", "/contacts", "/accounts", "/deals", 
                    "/activities", "/tasks", "/calendar", "/campaigns", "/emails", 
                    "/products", "/quotes", "/invoices", "/reports", "/analytics", 
                    "/settings", "/users", "/active-sessions"
                ))
                .build();

        LicenseEncryptionUtil.OpacTenant tenant = LicenseEncryptionUtil.OpacTenant.builder()
                .tenantName(request.getOrgCode().toLowerCase())
                .company(request.getOrgCode())
                .build();

        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String productsStr = objectMapper.writeValueAsString(java.util.Collections.singletonList(product));
            String signature = encryptionUtil.generateHmac(productsStr);

            LicenseEncryptionUtil.OpacLicensePayload payload = LicenseEncryptionUtil.OpacLicensePayload.builder()
                    .licenseVersion("1.0")
                    .issueDate(LocalDate.now().toString())
                    .expiryDate(request.getEndDate().toString())
                    .licenseType("Standard")
                    .tenant(tenant)
                    .products(java.util.Collections.singletonList(product))
                    .digitalSignature(signature)
                    .build();

            return encryptionUtil.encryptHex(objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test license key", e);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────

    private LicenseStatus computeStatus(LocalDate endDate, int graceDays) {
        LocalDate today = LocalDate.now();
        if (!today.isAfter(endDate)) return LicenseStatus.ACTIVE;
        long daysOver = ChronoUnit.DAYS.between(endDate, today);
        return daysOver <= graceDays ? LicenseStatus.GRACE : LicenseStatus.EXPIRED;
    }

    private java.util.List<String> parseFeatures(String json) {
        if (json == null || json.isBlank()) return java.util.Collections.emptyList();
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {});
        } catch (Exception ex) {
            return java.util.Collections.emptyList();
        }
    }

    private LicenseStatusResponse buildStatusResponse(CrmLicense lic, String orgName) {
        LocalDate today = LocalDate.now();
        long daysToExpiry = ChronoUnit.DAYS.between(today, lic.getEndDate());
        int graceDays = lic.getGracePeriodDays() != null ? lic.getGracePeriodDays() : 30;

        boolean inGrace = today.isAfter(lic.getEndDate())
                && ChronoUnit.DAYS.between(lic.getEndDate(), today) <= graceDays;

        int graceRemaining = 0;
        if (inGrace) {
            graceRemaining = (int)(graceDays - ChronoUnit.DAYS.between(lic.getEndDate(), today));
        }

        List<String> orgUsernames = userRepository.findByOrganizationId(lic.getOrganizationId())
                .stream().map(u -> u.getUsername()).toList();
        long activeSessions = sessionRepository.findByUsernameIn(orgUsernames).stream()
                .filter(s -> "ACTIVE".equals(s.getStatus()))
                .count();

        long currentUsers = orgUsernames.size();

        return LicenseStatusResponse.builder()
                .id(lic.getId())
                .organizationId(lic.getOrganizationId())
                .organizationName(orgName)
                .licenseName(lic.getLicenseName())
                .orgCode(lic.getOrgCode())
                .startDate(lic.getStartDate())
                .endDate(lic.getEndDate())
                .gracePeriodDays(graceDays)
                .maximumUsers(lic.getMaximumUsers())
                .concurrentUsers(lic.getConcurrentUsers())
                .status(computeStatus(lic.getEndDate(), graceDays))
                .daysRemaining((int) daysToExpiry)
                .inGracePeriod(inGrace)
                .graceRemaining(graceRemaining)
                .activeSessionCount((int) activeSessions)
                .currentUserCount((int) currentUsers)
                .activatedAt(lic.getCreatedAt())
                .features(parseFeatures(lic.getFeaturesJson()))
                .build();
    }
}
