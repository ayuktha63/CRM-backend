package com.orque.crm.auth.controller;

import com.orque.crm.auth.dto.*;
import com.orque.crm.auth.entity.Role;
import com.orque.crm.auth.entity.User;
import com.orque.crm.auth.repository.RoleRepository;
import com.orque.crm.auth.repository.UserRepository;
import com.orque.crm.common.UserContextHelper;
import com.orque.crm.enums.RoleType;
import com.orque.crm.license.repository.CrmLicenseRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@CrossOrigin
public class UserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CrmLicenseRepository licenseRepository;
    private final RestTemplate restTemplate;

    @Value("${opac.base-url}")
    private String opacBaseUrl;

    @Value("${internal.service-api-key:}")
    private String internalServiceApiKey;

    // ── List ──────────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        // SYSTEM_ADMIN → all users; Org admin → own org; Sales user → just themselves
        String orgId = UserContextHelper.scopedOrgId();
        if (UserContextHelper.isAdmin()) {
            if (orgId == null) {
                return ResponseEntity.ok(userRepository.findAll().stream().map(this::toResponse).toList());
            }
            return ResponseEntity.ok(userRepository.findByOrganizationId(orgId).stream().map(this::toResponse).toList());
        }
        User me = UserContextHelper.currentUser();
        return ResponseEntity.ok(me != null ? List.of(toResponse(me)) : List.of());
    }

    // ── Get by ID ─────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        User user = findUser(id);
        if (!UserContextHelper.canAccess(user.getOrganizationId(), user.getUsername())) {
            return ResponseEntity.status(403).body(Map.of("message", "Access denied."));
        }
        return ResponseEntity.ok(toResponse(user));
    }

    // ── Create ────────────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest req) {
        if (userRepository.existsByUsername(req.getUsername()))
            return ResponseEntity.badRequest().body(Map.of("message", "Username already exists."));
        if (userRepository.existsByEmail(req.getEmail()))
            return ResponseEntity.badRequest().body(Map.of("message", "Email already exists."));

        // Enforce system-wide license user limit
        var systemLicense = licenseRepository.findByOrganizationId("SYSTEM").orElse(null);
        if (systemLicense != null && systemLicense.getConcurrentUsers() != null) {
            long totalUsers = userRepository.count();
            if (totalUsers >= systemLicense.getConcurrentUsers()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "message", "User limit reached. Please contact system admin for upgrade the license."
                ));
            }
        }

        // Enforce license user limit for org-scoped admins
        String orgId = UserContextHelper.currentOrganizationId();
        if (orgId != null && !orgId.isBlank()) {
            var license = licenseRepository.findByOrganizationId(orgId).orElse(null);
            if (license != null && license.getMaximumUsers() != null) {
                long currentCount = userRepository.countByOrganizationId(orgId);
                if (currentCount >= license.getMaximumUsers()) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "message", "User limit reached. Your license allows a maximum of "
                            + license.getMaximumUsers() + " user(s)."
                    ));
                }
            }
        }

        Role role = resolveRole(req.getRole());
        User user = User.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .username(req.getUsername())
                .email(req.getEmail())
                .phone(req.getPhone())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(role)
                .organizationId(orgId)
                .status(req.getStatus() != null ? req.getStatus() : "ACTIVE")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(toResponse(userRepository.save(user)));
    }

    // ── Update ────────────────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req) {
        User user = findUser(id);
        if (!UserContextHelper.canAccess(user.getOrganizationId(), user.getUsername())) {
            return ResponseEntity.status(403).body(Map.of("message", "Access denied."));
        }

        if (req.getEmail() != null && userRepository.existsByEmailAndIdNot(req.getEmail(), id))
            return ResponseEntity.badRequest().body(Map.of("message", "Email already in use."));

        if (req.getFirstName() != null) user.setFirstName(req.getFirstName());
        if (req.getLastName() != null)  user.setLastName(req.getLastName());
        if (req.getEmail() != null)     user.setEmail(req.getEmail());
        if (req.getPhone() != null)     user.setPhone(req.getPhone());
        if (req.getRole() != null)      user.setRole(resolveRole(req.getRole()));
        if (req.getStatus() != null) {
            user.setStatus(req.getStatus());
            user.setEnabled("ACTIVE".equalsIgnoreCase(req.getStatus()));
        }
        user.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(toResponse(userRepository.save(user)));
    }

    // ── Reset Password ────────────────────────────────────────────────────
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest req) {
        User user = findUser(id);
        if (!UserContextHelper.canAccess(user.getOrganizationId(), user.getUsername())) {
            return ResponseEntity.status(403).body(Map.of("message", "Access denied."));
        }

        // OPAC is the actual login source of truth (CRM's own password column is never
        // checked at login), so the real reset must land there first.
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Api-Key", internalServiceApiKey);
            org.springframework.http.HttpEntity<Map<String, String>> entity = new org.springframework.http.HttpEntity<>(
                    Map.of("username", user.getUsername(), "newPassword", req.getNewPassword()), headers);
            restTemplate.postForEntity(opacBaseUrl + "/api/admin/users/reset-password", entity, Map.class);
        } catch (Exception e) {
            return ResponseEntity.status(502).body(Map.of("message", "Failed to reset password in OPAC: " + e.getMessage()));
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("success", true, "message", "Password reset successfully."));
    }

    // ── Activate ──────────────────────────────────────────────────────────
    @PostMapping("/{id}/activate")
    public ResponseEntity<?> activateUser(@PathVariable Long id) {
        User user = findUser(id);
        if (!UserContextHelper.isSystemAdmin() && !UserContextHelper.currentOrganizationId().equals(user.getOrganizationId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Access denied."));
        }
        user.setStatus("ACTIVE");
        user.setEnabled(true);
        user.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(toResponse(userRepository.save(user)));
    }

    // ── Deactivate ────────────────────────────────────────────────────────
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateUser(@PathVariable Long id) {
        User user = findUser(id);
        if (!UserContextHelper.isSystemAdmin() && !UserContextHelper.currentOrganizationId().equals(user.getOrganizationId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Access denied."));
        }
        user.setStatus("INACTIVE");
        user.setEnabled(false);
        user.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(toResponse(userRepository.save(user)));
    }

    // ── Delete ────────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        User user = findUser(id);
        if (!UserContextHelper.isSystemAdmin() && !UserContextHelper.currentOrganizationId().equals(user.getOrganizationId())) {
            return ResponseEntity.status(403).body(Map.of("message", "Access denied."));
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "User deleted."));
    }

    // ── Sales users list (for reassignment) ──────────────────────────────
    // SALES_USER is the actual role every tenant's business user gets synced in with
    // from OPAC — SALES/SALES_ADMIN are legacy/unused roles that no real tenant user
    // has, so querying only those left this list (and Bulk Assign) empty in practice.
    @GetMapping("/sales")
    public ResponseEntity<List<UserResponse>> getSalesUsers() {
        String orgId = UserContextHelper.scopedOrgId();
        java.util.stream.Stream<UserResponse> salesUsers = userRepository.findByRoleName(RoleType.SALES_USER).stream()
                .filter(u -> orgId == null || orgId.equals(u.getOrganizationId()))
                .map(this::toResponse);
        java.util.stream.Stream<UserResponse> sales = userRepository.findByRoleName(RoleType.SALES).stream()
                .filter(u -> orgId == null || orgId.equals(u.getOrganizationId()))
                .map(this::toResponse);
        java.util.stream.Stream<UserResponse> admins = userRepository.findByRoleName(RoleType.SALES_ADMIN).stream()
                .filter(u -> orgId == null || orgId.equals(u.getOrganizationId()))
                .map(this::toResponse);
        return ResponseEntity.ok(
                java.util.stream.Stream.concat(admins, java.util.stream.Stream.concat(sales, salesUsers)).toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    private Role resolveRole(String roleName) {
        RoleType type;
        try {
            type = RoleType.valueOf(roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = RoleType.SALES;
        }
        RoleType finalType = type;
        return roleRepository.findByName(finalType)
                .orElseGet(() -> roleRepository.save(Role.builder().name(finalType).build()));
    }

    UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole() != null ? user.getRole().getName().name() : null)
                .status(user.getStatus() != null ? user.getStatus() : (Boolean.TRUE.equals(user.getEnabled()) ? "ACTIVE" : "INACTIVE"))
                .enabled(user.getEnabled())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
