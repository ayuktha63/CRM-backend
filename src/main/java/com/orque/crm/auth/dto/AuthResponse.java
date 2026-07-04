package com.orque.crm.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;

    private String refreshToken;

    private String tokenType;

    private Long userId;

    private String username;

    private String email;

    private String role;

    /** Non-null when the org license is in grace period. Shown as a banner in the UI. */
    private String licenseWarning;

    /** Feature routes from the OPAC policy that was active at SSO time. Stored as accesspolicy in localStorage. */
    private java.util.List<String> accessPolicy;

    /** Tenant name from OPAC. Displayed in the topbar so users know which tenant they are operating in. */
    private String tenantName;

    /** CRM organization ID (UUID string). Sent so the frontend can include it in API payloads for explicit tenant filtering. */
    private String organizationId;
}