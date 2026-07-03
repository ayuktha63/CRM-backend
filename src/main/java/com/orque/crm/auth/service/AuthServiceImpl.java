package com.orque.crm.auth.service;

import com.orque.crm.audit.service.AuditLogService;
import com.orque.crm.auth.dto.*;
import com.orque.crm.auth.entity.Role;
import com.orque.crm.auth.entity.User;
import com.orque.crm.auth.repository.RoleRepository;
import com.orque.crm.auth.repository.UserRepository;
import com.orque.crm.common.ApiResponse;
import com.orque.crm.enums.AuditAction;
import com.orque.crm.enums.AuditModule;
import com.orque.crm.enums.OrganizationStatus;
import com.orque.crm.enums.RoleType;
import com.orque.crm.license.service.LicenseService;
import com.orque.crm.organization.entity.Organization;
import com.orque.crm.organization.repository.OrganizationRepository;
import com.orque.crm.security.JwtService;
import com.orque.crm.session.entity.UserSession;
import com.orque.crm.session.repository.UserSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    @Value("${opac.base-url:http://localhost:8082}")
    private String opacBaseUrl;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;
    private final UserSessionRepository sessionRepository;
    private final OrganizationRepository organizationRepository;
    private final LicenseService licenseService;

    @Override
    public ApiResponse register(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            return new ApiResponse(false, "Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            return new ApiResponse(false, "Email already exists");
        }

        Role role = roleRepository.findByName(RoleType.SALES_USER)
                .orElseThrow(() -> new RuntimeException("Default role not found"));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .role(role)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        return new ApiResponse(true, "User registered successfully");
    }

    @Override
    @SuppressWarnings("unchecked")
    public AuthResponse login(LoginRequest request) {
        // OPAC is the single source of truth for identity.
        // CRM never checks its own password store — it delegates to OPAC /api/auth/validate.
        java.util.Map<String, Object> opacResp = validateWithOpac(
                request.getUsernameOrEmail(), request.getPassword());
        if (!Boolean.TRUE.equals(opacResp.get("valid"))) {
            String reason = opacResp.getOrDefault("error", "Invalid credentials").toString();
            throw new RuntimeException(reason);
        }

        String username  = opacResp.getOrDefault("username", request.getUsernameOrEmail()).toString();
        String email     = opacResp.getOrDefault("email", "").toString();
        String firstName = opacResp.getOrDefault("firstName", username).toString();
        String lastName  = opacResp.getOrDefault("lastName", "").toString();
        String opacRole    = opacResp.getOrDefault("role", "REQUESTER").toString();
        String tenantName  = opacResp.getOrDefault("tenantName", "").toString();
        java.util.List<String> features = opacResp.get("features") instanceof java.util.List
                ? (java.util.List<String>) opacResp.get("features") : new java.util.ArrayList<>();

        // Find or auto-provision CRM user by username only (never fall back to email)
        RoleType crmRoleType = "SYSTEM_ADMIN".equals(opacRole) ? RoleType.SYSTEM_ADMIN : RoleType.SALES_USER;
        final RoleType finalRole = crmRoleType;
        User user = userRepository.findByUsername(username).orElseGet(() -> {
            Role role = roleRepository.findByName(finalRole)
                .orElseGet(() -> roleRepository.findByName(RoleType.SALES_USER)
                    .orElseThrow(() -> new RuntimeException("Default role not found")));
            String uniqueEmail = userRepository.existsByEmail(email) ? username + "+" + email : email;
            User newUser = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .username(username)
                .email(uniqueEmail)
                .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                .enabled(true)
                .role(role)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
            return userRepository.save(newUser);
        });

        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new RuntimeException("User account is disabled. Contact your administrator.");
        }

        // Org + license checks (skipped for users without an org assignment)
        String licenseWarning = null;
        String orgId = user.getOrganizationId();
        if (orgId != null && !orgId.isBlank()) {
            Organization org = organizationRepository.findById(orgId).orElse(null);
            if (org != null && org.getStatus() == OrganizationStatus.SUSPENDED) {
                throw new RuntimeException("Your organization account has been suspended. Contact support.");
            }
            LicenseService.LicenseCheckResult licResult = licenseService.check(orgId);
            if (licResult.allowed() && licResult.inGrace()) {
                licenseWarning = "License in grace period. " + licResult.graceRemaining() + " day(s) remaining.";
            }
        }

        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        auditLogService.createAudit(AuditAction.USER_LOGIN, AuditModule.AUTH,
                "User", user.getId(), null, "LOGIN",
                "User logged in via OPAC credentials", user.getUsername(), null);

        user.setLastLoginAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        HttpServletRequest httpReq = null;
        try {
            httpReq = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        } catch (Exception ignored) {}
        String ip     = httpReq != null ? httpReq.getRemoteAddr() : "unknown";
        String ua     = httpReq != null ? httpReq.getHeader("User-Agent") : "";
        String browser = parseBrowser(ua);
        String os      = parseOs(ua);
        String device  = ua != null && ua.toLowerCase().contains("mobile") ? "Mobile" : "Desktop";

        sessionRepository.save(UserSession.builder()
                .userId(user.getId()).username(user.getUsername())
                .role(user.getRole().getName().name()).jwtId(accessToken)
                .loginTime(LocalDateTime.now()).lastActivity(LocalDateTime.now())
                .ipAddress(ip).browser(browser).operatingSystem(os).device(device)
                .status("ACTIVE").build());

        return AuthResponse.builder()
                .accessToken(accessToken).refreshToken(refreshToken)
                .tokenType("Bearer").userId(user.getId())
                .username(user.getUsername()).email(user.getEmail())
                .role(user.getRole().getName().name())
                .accessPolicy(features)
                .tenantName(tenantName)
                .organizationId(user.getOrganizationId())
                .licenseWarning(licenseWarning)
                .build();
    }

    /** Delegates credential check to OPAC. Returns map with { valid, error?, username, email, role, features[] }. */
    @SuppressWarnings("unchecked")
    private java.util.Map<String, Object> validateWithOpac(String usernameOrEmail, String password) {
        try {
            org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
            java.util.Map<String, String> req = java.util.Map.of(
                "username", usernameOrEmail, "password", password);
            org.springframework.http.ResponseEntity<java.util.Map> resp = rt.postForEntity(
                opacBaseUrl + "/api/auth/validate", req, java.util.Map.class);
            java.util.Map<String, Object> body = resp.getBody();
            return body != null ? body : java.util.Map.of("valid", false, "error", "No response from OPAC");
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> errBody = om.readValue(
                    e.getResponseBodyAsString(), java.util.Map.class);
                return errBody != null ? errBody : java.util.Map.of("valid", false, "error", "Invalid credentials");
            } catch (Exception ignored) {}
            return java.util.Map.of("valid", false, "error", "Invalid credentials");
        } catch (Exception e) {
            log.warn("OPAC auth validation failed: {}", e.getMessage());
            return java.util.Map.of("valid", false, "error", "Authentication service unavailable");
        }
    }

    private String parseBrowser(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("Edg"))     return "Edge";
        if (ua.contains("Chrome"))  return "Chrome";
        if (ua.contains("Firefox")) return "Firefox";
        if (ua.contains("Safari"))  return "Safari";
        return "Other";
    }

    private String parseOs(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Mac"))     return "macOS";
        if (ua.contains("Linux"))   return "Linux";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone") || ua.contains("iPad")) return "iOS";
        return "Other";
    }

    @Override
    public UserProfileResponse getCurrentUser() {

        User user = (User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        return UserProfileResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().getName().name())
                .enabled(user.getEnabled())
                .build();
    }

    @Override
    public AuthResponse ssoLogin(String ssoToken) {
        // Validate token with OPAC backend
        try {
            org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
            java.util.Map<String, String> req = java.util.Map.of("token", ssoToken);
            org.springframework.http.ResponseEntity<java.util.Map> resp = rt.postForEntity(
                opacBaseUrl + "/api/sso/validate", req, java.util.Map.class);
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> body = (java.util.Map<String, Object>) resp.getBody();
            if (body == null || !Boolean.TRUE.equals(body.get("valid"))) {
                throw new RuntimeException("Invalid SSO token");
            }
            String username   = (String) body.get("username");
            String email      = (String) body.get("email");
            String opacRole   = body.getOrDefault("opacRole", "REQUESTER") != null
                ? body.getOrDefault("opacRole", "REQUESTER").toString() : "REQUESTER";
            String firstName  = body.getOrDefault("firstName", username) != null
                ? body.getOrDefault("firstName", username).toString() : username;
            String lastName    = body.getOrDefault("lastName", "") != null
                ? body.getOrDefault("lastName", "").toString() : "";
            String tenantName  = body.getOrDefault("tenantName", "") != null
                ? body.getOrDefault("tenantName", "").toString() : "";
            @SuppressWarnings("unchecked")
            java.util.List<String> features = body.get("features") instanceof java.util.List
                ? (java.util.List<String>) body.get("features") : new java.util.ArrayList<>();

            // Map OPAC role → CRM role
            RoleType crmRoleType = "SYSTEM_ADMIN".equals(opacRole) ? RoleType.SYSTEM_ADMIN : RoleType.SALES_USER;

            // For non-ORQUE tenants, find or create a CRM org for this tenant.
            // Platform owner (ORQUE) has no org — they are true system admins.
            final boolean isPlatformOwner = "ORQUE".equalsIgnoreCase(tenantName) || tenantName.isBlank();
            final String tenantOrgId;
            if (!isPlatformOwner) {
                String orgCode = tenantName.toUpperCase();
                Organization tenantOrg = organizationRepository.findByOrganizationCode(orgCode).orElseGet(() -> {
                    // No org with this code yet — check if the user already belongs to an org
                    // (e.g. stale "DEFAULT" org) and update its code in place rather than
                    // creating a duplicate.
                    User existingUser = userRepository.findByUsername(username).orElse(null);
                    if (existingUser != null && existingUser.getOrganizationId() != null) {
                        Organization existingOrg = organizationRepository.findById(existingUser.getOrganizationId()).orElse(null);
                        if (existingOrg != null) {
                            existingOrg.setOrganizationCode(orgCode);
                            existingOrg.setOrganizationName(tenantName);
                            return organizationRepository.save(existingOrg);
                        }
                    }
                    Organization newOrg = Organization.builder()
                            .organizationCode(orgCode)
                            .organizationName(tenantName)
                            .status(OrganizationStatus.ACTIVE)
                            .build();
                    return organizationRepository.save(newOrg);
                });
                tenantOrgId = tenantOrg.getId();
            } else {
                tenantOrgId = null;
            }

            // Find or auto-create the CRM user by OPAC username only (never fall back to email
            // — two OPAC users can share an email and must remain separate CRM accounts)
            final RoleType finalCrmRoleType = crmRoleType;
            User user = userRepository.findByUsername(username).orElseGet(() -> {
                Role role = roleRepository.findByName(finalCrmRoleType)
                    .orElseGet(() -> roleRepository.findByName(RoleType.SALES_USER)
                        .orElseThrow(() -> new RuntimeException("Default role not found")));
                // If the email is already taken by another CRM account, make it unique
                String uniqueEmail = userRepository.existsByEmail(email)
                    ? username + "+" + email
                    : email;
                User newUser = User.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .username(username)
                    .email(uniqueEmail)
                    .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                    .enabled(true)
                    .role(role)
                    .organizationId(tenantOrgId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
                return userRepository.save(newUser);
            });

            // Always keep org in sync with the OPAC tenant — a user could previously
            // have landed in DEFAULT org before the tenant org was set up correctly.
            if (tenantOrgId != null && !tenantOrgId.equals(user.getOrganizationId())) {
                user.setOrganizationId(tenantOrgId);
            }

            if (Boolean.FALSE.equals(user.getEnabled())) {
                throw new RuntimeException("User account is disabled");
            }

            String accessToken  = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().getName().name())
                .accessPolicy(features)
                .tenantName(tenantName)
                .build();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("SSO validation failed: " + e.getMessage());
        }
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {

        String refreshToken = request.getRefreshToken();

        String username = jwtService.extractUsername(refreshToken);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String newAccessToken = jwtService.generateAccessToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().getName().name())
                .build();
    }

    @Override
    public ApiMessageResponse logout() {

        Object principal = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        if (principal instanceof User user) {

            auditLogService.createAudit(
                    AuditAction.USER_LOGOUT,
                    AuditModule.AUTH,
                    "User",
                    user.getId(),
                    "LOGGED_IN",
                    "LOGGED_OUT",
                    "User logged out successfully",
                    user.getUsername(),
                    null
            );

            try {
                org.springframework.web.context.request.ServletRequestAttributes attrs = 
                    (org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes();
                jakarta.servlet.http.HttpServletRequest httpReq = attrs.getRequest();
                String authHeader = httpReq.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String jwt = authHeader.substring(7).trim();
                    sessionRepository.findByJwtId(jwt).ifPresent(sessionRepository::delete);
                }
            } catch (Exception ignored) { /* context not available */ }
        }

        return new ApiMessageResponse("Logged out successfully");
    }
}