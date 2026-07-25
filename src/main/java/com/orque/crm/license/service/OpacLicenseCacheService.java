package com.orque.crm.license.service;

import com.orque.crm.config.RedisCacheConfig;
import com.orque.crm.enums.LicenseStatus;
import com.orque.crm.license.dto.LicenseStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Checks OPAC's master license for an org over HTTP. This is a cross-service call
 * that LicenseValidationFilter can trigger on nearly every authenticated CRM
 * request when the org has no local CRM license row — Redis-cached since license
 * status only changes on activation/renewal (a rare, human-initiated action on
 * the OPAC side).
 *
 * Lives in its own bean (not a private method on LicenseServiceImpl) because
 * Spring's proxy-based @Cacheable cannot intercept self-invoked private calls.
 */
@Slf4j
@Service
public class OpacLicenseCacheService {

    @Value("${opac.base-url}")
    private String opacBaseUrl;

    private final RestTemplate restTemplate;

    public OpacLicenseCacheService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Cacheable(value = RedisCacheConfig.CACHE_OPAC_LICENSE, key = "#orgCode", unless = "#result == null")
    @SuppressWarnings("unchecked")
    public LicenseStatusResponse checkOpacMasterLicense(String orgCode, String organizationId, String orgName) {
        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(
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
            List<String> features = body.get("features") instanceof List ? (List<String>) body.get("features") : new ArrayList<>();

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

    /**
     * Per-user seat (sub-license) a tenant's System Admin issued to this specific
     * person — distinct from checkOpacMasterLicense above, which is the tenant-wide
     * license every user in the org shares. Individual users should see their own
     * seat's expiry/features here, not the org's master license.
     */
    @Cacheable(value = RedisCacheConfig.CACHE_OPAC_LICENSE, key = "'seat:' + #orgCode + ':' + #username", unless = "#result == null")
    @SuppressWarnings("unchecked")
    public LicenseStatusResponse checkOpacUserSeat(String username, String orgCode, String organizationId, String orgName) {
        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(
                    opacBaseUrl + "/api/internal/crm-user-seat/" + orgCode + "/" + username, Map.class);
            Map<String, Object> body = resp.getBody();
            if (body == null || !Boolean.TRUE.equals(body.get("active"))) return null;

            boolean inGrace = Boolean.TRUE.equals(body.get("inGrace"));
            int daysRemaining = body.get("daysRemaining") instanceof Number ? ((Number) body.get("daysRemaining")).intValue() : 0;
            int graceRemaining = body.get("graceRemaining") instanceof Number ? ((Number) body.get("graceRemaining")).intValue() : 0;
            int gracePeriod = body.get("gracePeriod") instanceof Number ? ((Number) body.get("gracePeriod")).intValue() : 30;
            String expiry = body.getOrDefault("expiry", "").toString();
            String activatedOn = body.getOrDefault("activatedOn", "").toString();
            List<String> features = body.get("features") instanceof List ? (List<String>) body.get("features") : new ArrayList<>();

            LocalDate endDate = expiry.isBlank() ? LocalDate.now() : LocalDate.parse(expiry);
            LicenseStatus status = inGrace ? LicenseStatus.GRACE : LicenseStatus.ACTIVE;
            LocalDateTime activatedAt = (activatedOn.length() == 10) ? LocalDate.parse(activatedOn).atStartOfDay() : null;

            return LicenseStatusResponse.builder()
                    .organizationId(organizationId)
                    .organizationName(orgName)
                    .licenseName("Individual Seat License")
                    .orgCode(orgCode)
                    .endDate(endDate)
                    .gracePeriodDays(gracePeriod)
                    .status(status)
                    .daysRemaining(daysRemaining)
                    .inGracePeriod(inGrace)
                    .graceRemaining(graceRemaining)
                    .activatedAt(activatedAt)
                    .features(features)
                    .build();
        } catch (Exception e) {
            log.warn("Could not reach OPAC for user seat check (org={}, user={}): {}", orgCode, username, e.getMessage());
            return null;
        }
    }
}
