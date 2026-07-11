package com.orque.crm.auth.service;

import com.orque.crm.auth.dto.*;
import com.orque.crm.common.ApiResponse;

import java.util.Map;

public interface AuthService {

    ApiResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse ssoLogin(String ssoToken);
    UserProfileResponse getCurrentUser();
    AuthResponse refreshToken(RefreshTokenRequest request);
    ApiMessageResponse logout();

    /** Provisions (or skips, if already present) a CRM user pushed directly from OPAC's user creation flow. */
    Map<String, Object> syncUserFromOpac(Map<String, String> body);

    /** Always returns a generic message regardless of whether the email is registered. */
    ApiMessageResponse forgotPassword(ForgotPasswordRequest request);

    ApiMessageResponse resetPassword(ResetPasswordConfirmRequest request);

    /** { valid: boolean, maskedEmail?: string } — lets the reset screen show whose account this is. */
    Map<String, Object> validateResetToken(String token);
}