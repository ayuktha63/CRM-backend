package com.orque.crm.auth.controller;

import com.orque.crm.auth.dto.*;
import com.orque.crm.auth.entity.Role;
import com.orque.crm.auth.entity.User;
import com.orque.crm.auth.repository.RoleRepository;
import com.orque.crm.auth.repository.UserRepository;
import com.orque.crm.enums.RoleType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

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

    // ── List ──────────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User u) {
            String roleName = u.getRole() != null ? u.getRole().getName().name() : "";
            if ("ADMIN".equals(roleName) || "SALES_ADMIN".equals(roleName)) {
                return ResponseEntity.ok(userRepository.findAll().stream().map(this::toResponse).toList());
            } else {
                return ResponseEntity.ok(List.of(toResponse(u)));
            }
        }
        return ResponseEntity.ok(userRepository.findAll().stream().map(this::toResponse).toList());
    }

    // ── Get by ID ─────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        User user = findUser(id);
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User u) {
            String roleName = u.getRole() != null ? u.getRole().getName().name() : "";
            if (!"ADMIN".equals(roleName) && !"SALES_ADMIN".equals(roleName) && !u.getId().equals(id)) {
                return ResponseEntity.status(403).body(Map.of("message", "Access denied."));
            }
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

        Role role = resolveRole(req.getRole());
        User user = User.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .username(req.getUsername())
                .email(req.getEmail())
                .phone(req.getPhone())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(role)
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
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("success", true, "message", "Password reset successfully."));
    }

    // ── Activate ──────────────────────────────────────────────────────────
    @PostMapping("/{id}/activate")
    public ResponseEntity<UserResponse> activateUser(@PathVariable Long id) {
        User user = findUser(id);
        user.setStatus("ACTIVE");
        user.setEnabled(true);
        user.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(toResponse(userRepository.save(user)));
    }

    // ── Deactivate ────────────────────────────────────────────────────────
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable Long id) {
        User user = findUser(id);
        user.setStatus("INACTIVE");
        user.setEnabled(false);
        user.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(toResponse(userRepository.save(user)));
    }

    // ── Delete ────────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        findUser(id);
        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "User deleted."));
    }

    // ── Sales users list (for reassignment) ──────────────────────────────
    @GetMapping("/sales")
    public ResponseEntity<List<UserResponse>> getSalesUsers() {
        List<UserResponse> sales = userRepository.findByRoleName(RoleType.SALES).stream()
                .map(this::toResponse).toList();
        List<UserResponse> admins = userRepository.findByRoleName(RoleType.SALES_ADMIN).stream()
                .map(this::toResponse).toList();
        return ResponseEntity.ok(
                java.util.stream.Stream.concat(admins.stream(), sales.stream()).toList());
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
