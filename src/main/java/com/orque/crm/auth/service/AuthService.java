package com.orque.crm.auth.service;

import com.orque.crm.auth.dto.*;
import com.orque.crm.common.ApiResponse;

public interface AuthService {

    ApiResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserProfileResponse getCurrentUser();
    AuthResponse refreshToken(RefreshTokenRequest request);
    ApiMessageResponse logout();
}