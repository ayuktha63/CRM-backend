package com.orque.crm.session.controller;

import com.orque.crm.auth.entity.User;
import com.orque.crm.auth.repository.UserRepository;
import com.orque.crm.common.UserContextHelper;
import com.orque.crm.security.JwtService;
import com.orque.crm.session.dto.SessionResponse;
import com.orque.crm.session.entity.UserSession;
import com.orque.crm.session.repository.UserSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
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
    private final UserRepository userRepository;
    private final JwtService jwtService;

    /**
     * Authenticated no-op the frontend polls on an interval so a terminated/expired
     * session gets caught (and the grace-period popup shown) even on an idle screen
     * that isn't otherwise making API calls. 401s here are handled the same as any
     * other request by JwtAuthenticationFilter — no extra logic needed.
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping
    public ResponseEntity<List<SessionResponse>> getAllSessions() {
        sessionRepository.expireOldSessions(LocalDateTime.now().minusHours(8));

        List<UserSession> sessions;
        String orgId = UserContextHelper.scopedOrgId();
        if (orgId == null) {
            // SYSTEM_ADMIN — all sessions
            sessions = sessionRepository.findAll();
        } else {
            // Scope to usernames belonging to this org
            List<String> orgUsernames = userRepository.findByOrganizationId(orgId)
                    .stream().map(User::getUsername).toList();
            sessions = sessionRepository.findByUsernameIn(orgUsernames);
        }

        return ResponseEntity.ok(
                sessions.stream()
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
    public ResponseEntity<?> getById(@PathVariable Long id) {
        UserSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found: " + id));
        if (!sessionInScope(session)) {
            return ResponseEntity.status(403).build();
        }
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
        if (!sessionInScope(session)) {
            return ResponseEntity.status(403).body(Map.of("message", "Access denied."));
        }
        // Mark TERMINATED rather than delete: JwtAuthenticationFilter only blocks a JWT
        // when its session row explicitly shows TERMINATED/EXPIRED — deleting the row
        // makes findByJwtId() come back empty, which the filter treats as "no session
        // tracked yet" and lets the token through, defeating the whole point of terminate.
        session.setStatus("TERMINATED");
        session.setLogoutTime(LocalDateTime.now());
        sessionRepository.save(session);
        return ResponseEntity.ok(Map.of("success", true, "message", "Session terminated."));
    }

    @DeleteMapping("/terminate-all")
    public ResponseEntity<Map<String, Object>> terminateAll() {
        String currentJwt = getCurrentJwt();
        List<UserSession> active = sessionRepository.findByStatus("ACTIVE");
        List<UserSession> toTerminate = active.stream()
                .filter(s -> !s.getJwtId().equals(currentJwt))
                .filter(this::sessionInScope)
                .toList();
        LocalDateTime now = LocalDateTime.now();
        toTerminate.forEach(s -> {
            s.setStatus("TERMINATED");
            s.setLogoutTime(now);
        });
        sessionRepository.saveAll(toTerminate);
        return ResponseEntity.ok(Map.of("success", true, "message",
                "All other sessions terminated."));
    }

    /**
     * Lets a client "continue" a session that was just TERMINATED (by an admin) or
     * EXPIRED (by inactivity) without forcing an immediate logout — used by the frontend's
     * grace-period popup shown on any 401. Deliberately permitAll'd (see SecurityConfig):
     * the normal auth filter already refuses to authenticate a request carrying a
     * TERMINATED/EXPIRED session's JWT, so this endpoint has to validate the token's
     * signature/expiry itself, independent of the blocked session row, before flipping
     * it back to ACTIVE.
     */
    @PostMapping("/resume")
    public ResponseEntity<Map<String, Object>> resumeSession(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "No token provided."));
        }
        String jwt = authHeader.substring(7).trim();

        if (!jwtService.isTokenSignatureValid(jwt)) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Session has expired. Please log in again."));
        }

        sessionRepository.findByJwtId(jwt).ifPresent(session -> {
            if ("TERMINATED".equals(session.getStatus()) || "EXPIRED".equals(session.getStatus())) {
                session.setStatus("ACTIVE");
                session.setLogoutTime(null);
                session.setLastActivity(LocalDateTime.now());
                sessionRepository.save(session);
            }
        });

        return ResponseEntity.ok(Map.of("success", true, "message", "Session resumed."));
    }

    private String getCurrentJwt() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof User u) return u.getUsername();
        } catch (Exception ignored) { /* no active session */ }
        return "";
    }

    /** True if the session belongs to the caller's org (or caller is SYSTEM_ADMIN). */
    private boolean sessionInScope(UserSession session) {
        String orgId = UserContextHelper.scopedOrgId();
        if (orgId == null) return true;
        List<String> orgUsernames = userRepository.findByOrganizationId(orgId)
                .stream().map(User::getUsername).toList();
        return orgUsernames.contains(session.getUsername());
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
