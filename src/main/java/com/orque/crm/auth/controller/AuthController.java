package com.orque.crm.auth.controller;

import com.orque.crm.auth.dto.*;
import com.orque.crm.auth.service.AuthService;
import com.orque.crm.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.orque.crm.auth.dto.ApiMessageResponse;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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