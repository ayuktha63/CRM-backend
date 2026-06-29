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
import com.orque.crm.enums.RoleType;
import com.orque.crm.security.JwtService;
import com.orque.crm.session.entity.UserSession;
import com.orque.crm.session.repository.UserSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;
    private final UserSessionRepository sessionRepository;

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
    public AuthResponse login(LoginRequest request) {

        User user = userRepository
                .findByUsernameOrEmail(
                        request.getUsernameOrEmail(),
                        request.getUsernameOrEmail()
                )
                .orElseThrow(() ->
                        new RuntimeException("Invalid username or email"));

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword()
                );

        if (!passwordMatches) {
            throw new RuntimeException("Invalid password");
        }

        String accessToken =
                jwtService.generateAccessToken(user);

        String refreshToken =
                jwtService.generateRefreshToken(user);

        auditLogService.createAudit(
                AuditAction.USER_LOGIN,
                AuditModule.AUTH,
                "User",
                user.getId(),
                null,
                "LOGIN",
                "User logged in successfully",
                user.getUsername(),
                null
        );

        // Update lastLoginAt
        user.setLastLoginAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Record session
        HttpServletRequest httpReq = null;
        try {
            httpReq = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        } catch (Exception ignored) { /* request context not available outside web thread */ }
        String ip       = httpReq != null ? httpReq.getRemoteAddr() : "unknown";
        String ua       = httpReq != null ? httpReq.getHeader("User-Agent") : "";
        String browser  = parseBrowser(ua);
        String os       = parseOs(ua);
        String device   = ua != null && ua.toLowerCase().contains("mobile") ? "Mobile" : "Desktop";

        sessionRepository.save(UserSession.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().getName().name())
                .jwtId(accessToken)
                .loginTime(LocalDateTime.now())
                .lastActivity(LocalDateTime.now())
                .ipAddress(ip)
                .browser(browser)
                .operatingSystem(os)
                .device(device)
                .status("ACTIVE")
                .build());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().getName().name())
                .build();
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