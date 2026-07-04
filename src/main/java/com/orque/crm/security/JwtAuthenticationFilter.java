package com.orque.crm.security;

import com.orque.crm.auth.entity.User;
import com.orque.crm.auth.repository.UserRepository;
import com.orque.crm.session.repository.UserSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    )
            throws ServletException,
            IOException {

        final String authHeader =
                request.getHeader("Authorization");

        // The browser's EventSource API (used for the notification SSE stream) can't
        // set custom headers, so it passes the JWT as a query param instead. Only
        // accepted on that one path — every other endpoint still requires the header.
        String jwt = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7).trim();
        } else if ("/api/v1/notifications/stream".equals(request.getServletPath())) {
            String tokenParam = request.getParameter("token");
            if (tokenParam != null && !tokenParam.isBlank()) {
                jwt = tokenParam.trim();
            }
        }

        if (jwt == null) {
            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        String username = null;
        try {
            username = jwtService.extractUsername(jwt);
        } catch (Exception e) {
            System.err.println("JWT extraction failed: " + e.getMessage());
        }

        if (username != null &&
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        == null) {

            User user =
                    userRepository
                            .findByUsername(username)
                            .orElse(null);

            // Allow if no session row exists (pre-sessions-feature tokens) or row is ACTIVE.
            // Only block when a row explicitly shows TERMINATED or EXPIRED.
            boolean isSessionBlocked = sessionRepository.findByJwtId(jwt)
                    .map(session -> "TERMINATED".equals(session.getStatus()) || "EXPIRED".equals(session.getStatus()))
                    .orElse(false);

            if (user != null &&
                    !isSessionBlocked &&
                    jwtService
                            .isTokenValid(
                                    jwt,
                                    user
                            )) {

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                null
                        );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(
                                authToken
                        );

                // Update lastActivity for active session
                sessionRepository.findByJwtId(jwt).ifPresent(session -> {
                    session.setLastActivity(LocalDateTime.now());
                    sessionRepository.save(session);
                });
            }
        }

        filterChain.doFilter(
                request,
                response
        );
    }
}