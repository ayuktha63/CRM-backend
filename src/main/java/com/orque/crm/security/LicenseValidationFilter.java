package com.orque.crm.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orque.crm.common.UserContextHelper;
import com.orque.crm.license.service.LicenseService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Runs after JWT authentication. For every authenticated request:
 *  1. SYSTEM_ADMIN → bypass all license checks.
 *  2. Otherwise → check the org's license (expired / grace / active).
 *  3. If expired → 402 Payment Required with a descriptive message.
 *  4. If grace  → allow through, adds X-License-Warning header.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LicenseValidationFilter extends OncePerRequestFilter {

    private final LicenseService licenseService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // Only validate authenticated requests
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            chain.doFilter(request, response);
            return;
        }

        // Bypass license checks for activation, status, settings license decryption, notifications, and search
        String path = request.getServletPath();
        if (path.startsWith("/api/v1/license/activate") || 
            path.startsWith("/api/v1/license/status") ||
            path.startsWith("/api/v1/settings/license") ||
            path.startsWith("/api/v1/notifications") ||
            path.startsWith("/api/v1/search")) {
            chain.doFilter(request, response);
            return;
        }

        // Check system license for SYSTEM_ADMIN (fail-closed if missing/expired)
        if (UserContextHelper.isSystemAdmin()) {
            LicenseService.LicenseCheckResult systemResult = licenseService.check("SYSTEM");
            if (!systemResult.allowed()) {
                log.warn("System license check failed for SYSTEM_ADMIN: {}", systemResult.message());
                response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                        "success", false,
                        "message", systemResult.message(),
                        "licenseExpired", true
                )));
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        String orgId = UserContextHelper.currentOrganizationId();
        LicenseService.LicenseCheckResult result = licenseService.check(orgId);

        if (!result.allowed()) {
            log.warn("License check failed for org={}: {}", orgId, result.message());
            response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                    "success", false,
                    "message", result.message(),
                    "licenseExpired", true
            )));
            return;
        }

        // Feature access control check
        String feature = getRequiredFeature(path);
        if (feature != null) {
            boolean hasFeature = checkFeatureAllowed(orgId, feature);
            if (!hasFeature) {
                log.warn("Access denied to feature '{}' for organization '{}'", feature, orgId);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                        "success", false,
                        "message", "This feature is not enabled under your current license."
                )));
                return;
            }
        }

        if (result.inGrace()) {
            response.setHeader("X-License-Warning",
                    "License in grace period. " + result.graceRemaining() + " day(s) remaining.");
        }

        chain.doFilter(request, response);
    }

    private String getRequiredFeature(String path) {
        if (path.startsWith("/api/v1/leads")) return "/leads";
        if (path.startsWith("/api/v1/contacts")) return "/contacts";
        if (path.startsWith("/api/v1/accounts")) return "/accounts";
        if (path.startsWith("/api/v1/deals")) return "/deals";
        if (path.startsWith("/api/v1/activities")) return "/activities";
        if (path.startsWith("/api/v1/tasks")) return "/tasks";
        if (path.startsWith("/api/v1/calendar")) return "/calendar";
        if (path.startsWith("/api/v1/campaigns")) return "/campaigns";
        if (path.startsWith("/api/v1/emails")) return "/emails";
        if (path.startsWith("/api/v1/products")) return "/products";
        if (path.startsWith("/api/v1/quotes")) return "/quotes";
        if (path.startsWith("/api/v1/invoices")) return "/invoices";
        if (path.startsWith("/api/v1/reports")) return "/reports";
        if (path.startsWith("/api/v1/report-builder")) return "/report-builder";
        if (path.startsWith("/api/v1/analytics")) return "/analytics";
        if (path.startsWith("/api/v1/customization")) return "/customization";
        if (path.startsWith("/api/v1/dashboard-builder")) return "/dashboard-builder";
        if (path.startsWith("/api/v1/settings")) return "/settings";
        if (path.startsWith("/api/v1/users")) return "/users";
        if (path.startsWith("/api/v1/sessions") || path.startsWith("/api/v1/active-sessions")) return "/active-sessions";
        return null;
    }

    private boolean checkFeatureAllowed(String orgId, String feature) {
        if (orgId == null || "SYSTEM".equalsIgnoreCase(orgId)) return true;
        try {
            var status = licenseService.getStatus(orgId);
            if (status == null || status.getFeatures() == null) return false;
            return status.getFeatures().contains(feature);
        } catch (Exception e) {
            return false;
        }
    }

    /** Skip license check for auth endpoints. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/v1/auth/")
                || path.startsWith("/api/v1/google/")
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/v3/api-docs/");
    }
}
