package com.orque.crm.auth.controller;

import com.orque.crm.auth.dto.*;
import com.orque.crm.auth.entity.Role;
import com.orque.crm.auth.entity.User;
import com.orque.crm.auth.repository.RoleRepository;
import com.orque.crm.auth.repository.UserRepository;
import com.orque.crm.auth.service.AuthService;
import com.orque.crm.common.ApiResponse;
import com.orque.crm.enums.RoleType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.orque.crm.auth.dto.ApiMessageResponse;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // Self-registration is disabled. All CRM users are provisioned via OPAC user_master.
    // Users authenticate through /login (delegates to OPAC) or /sso (SSO token from OPAC).
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.status(403).body(new ApiResponse(false,
            "Self-registration is not allowed. Contact your system administrator to create your account in OPAC."));
    }
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        AuthResponse response =
                authService.login(request);

        return ResponseEntity.ok(response);
    }
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser() {

        UserProfileResponse response =
                authService.getCurrentUser();

        return ResponseEntity.ok(response);
    }
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {

        AuthResponse response =
                authService.refreshToken(request);

        return ResponseEntity.ok(response);
    }
    /**
     * Internal endpoint called by OPAC after creating a new user.
     * No JWT required — secured by header check only.
     */
    @PostMapping("/sync-user")
    public ResponseEntity<?> syncUser(
            @RequestHeader(value = "X-Internal-Sync", required = false) String syncHeader,
            @RequestBody Map<String, String> body) {
        if (!"true".equals(syncHeader)) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden"));
        }
        String username = body.get("username");
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "username required"));
        }
        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.ok(Map.of("message", "User already exists", "skipped", true));
        }
        String opacRole = body.getOrDefault("role", "SALES_USER");
        RoleType roleType = "SYSTEM_ADMIN".equals(opacRole) ? RoleType.SYSTEM_ADMIN : RoleType.SALES_USER;
        Role role = roleRepository.findByName(roleType)
                .orElseGet(() -> roleRepository.findByName(RoleType.SALES_USER)
                        .orElseThrow(() -> new RuntimeException("Default role not found")));
        String email = body.getOrDefault("email", "");
        String uniqueEmail = userRepository.existsByEmail(email) ? username + "+" + email : email;
        User newUser = User.builder()
                .firstName(body.getOrDefault("firstName", username))
                .lastName(body.getOrDefault("lastName", "-"))
                .username(username)
                .email(uniqueEmail)
                .phone(body.get("phone"))
                .password(passwordEncoder.encode(body.getOrDefault("password", java.util.UUID.randomUUID().toString())))
                .role(role)
                .status("ACTIVE")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(newUser);
        return ResponseEntity.ok(Map.of("message", "User synced", "username", username));
    }

    @PostMapping("/sso")
    public ResponseEntity<AuthResponse> ssoLogin(
            @RequestBody java.util.Map<String, String> body
    ) {
        String token = body.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        AuthResponse response = authService.ssoLogin(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiMessageResponse> logout() {

        ApiMessageResponse response =
                authService.logout();

        return ResponseEntity.ok(response);
    }
}