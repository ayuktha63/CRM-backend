package com.orque.crm.session.controller;

import com.orque.crm.auth.entity.User;
import com.orque.crm.session.dto.SessionResponse;
import com.orque.crm.session.entity.UserSession;
import com.orque.crm.session.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@CrossOrigin
public class SessionController {

    private final UserSessionRepository sessionRepository;

    @GetMapping
    public ResponseEntity<List<SessionResponse>> getAllSessions() {
        // Auto-expire sessions idle for > 8 hours before returning list
        sessionRepository.expireOldSessions(LocalDateTime.now().minusHours(8));
        return ResponseEntity.ok(
                sessionRepository.findAll().stream()
                        .sorted((a, b) -> {
                            LocalDateTime ta = a.getLastActivity() != null ? a.getLastActivity() : LocalDateTime.MIN;
                            LocalDateTime tb = b.getLastActivity() != null ? b.getLastActivity() : LocalDateTime.MIN;
                            return tb.compareTo(ta);
                        })
                        .map(this::toResponse)
                        .toList()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> getById(@PathVariable Long id) {
        UserSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found: " + id));
        return ResponseEntity.ok(toResponse(session));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        sessionRepository.expireOldSessions(LocalDateTime.now().minusHours(8));
        long active   = sessionRepository.countActiveSessions();
        long total    = sessionRepository.count();
        long logins   = sessionRepository.countLoginsToday(LocalDateTime.now().withHour(0).withMinute(0).withSecond(0));
        long offline  = total - active;
        return ResponseEntity.ok(Map.of(
                "activeSessions",  active,
                "onlineUsers",     active,
                "offlineUsers",    offline,
                "todayLogins",     logins,
                "totalUsers",      total
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> terminateSession(@PathVariable Long id) {
        UserSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found: " + id));
        sessionRepository.delete(session);
        return ResponseEntity.ok(Map.of("success", true, "message", "Session terminated."));
    }

    @DeleteMapping("/terminate-all")
    public ResponseEntity<Map<String, Object>> terminateAll() {
        String currentJwt = getCurrentJwt();
        List<UserSession> active = sessionRepository.findByStatus("ACTIVE");
        List<UserSession> toDelete = active.stream()
                .filter(s -> !s.getJwtId().equals(currentJwt))
                .toList();
        sessionRepository.deleteAll(toDelete);
        return ResponseEntity.ok(Map.of("success", true, "message",
                "All other sessions terminated."));
    }

    private String getCurrentJwt() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof User u) return u.getUsername();
        } catch (Exception ignored) { /* no active session */ }
        return "";
    }

    private SessionResponse toResponse(UserSession s) {
        long minutes = 0;
        if (s.getLoginTime() != null) {
            LocalDateTime end = s.getLogoutTime() != null ? s.getLogoutTime() : LocalDateTime.now();
            minutes = Duration.between(s.getLoginTime(), end).toMinutes();
        }
        return SessionResponse.builder()
                .id(s.getId())
                .userId(s.getUserId())
                .username(s.getUsername())
                .role(s.getRole())
                .loginTime(s.getLoginTime())
                .lastActivity(s.getLastActivity())
                .logoutTime(s.getLogoutTime())
                .ipAddress(s.getIpAddress())
                .browser(s.getBrowser())
                .operatingSystem(s.getOperatingSystem())
                .device(s.getDevice())
                .status(s.getStatus())
                .durationMinutes(minutes)
                .build();
    }
}
