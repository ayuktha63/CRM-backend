package com.orque.crm.google.auth;

import com.orque.crm.common.UserContextHelper;
import com.orque.crm.google.entity.GoogleWorkspaceCredential;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;
import java.util.Map;

/**
 * Single OAuth connection covering Gmail, Calendar (+ Meet), and Tasks — there is no per-service
 * connect flow. See {@link com.orque.crm.google.token.GoogleTokenManager} for how every other
 * google/* module turns this one stored connection into a live, auto-refreshing API client.
 */
@RestController
@RequestMapping("/api/v1/google/auth")
@RequiredArgsConstructor
public class GoogleWorkspaceAuthController {

    private final GoogleWorkspaceAuthService authService;

    /** Returns the Google consent-screen URL for the current logged-in user to connect their own account. */
    @GetMapping("/url")
    public ResponseEntity<Map<String, String>> getAuthorizationUrl() {
        String username = UserContextHelper.currentUsername();
        return ResponseEntity.ok(Map.of("url", authService.generateAuthorizationUrl(username)));
    }

    /** Google redirects the browser here directly (no auth header) — user identity comes from the signed state param. */
    @GetMapping("/callback")
    public RedirectView handleCallback(@RequestParam("code") String code, @RequestParam("state") String state) {
        return new RedirectView(authService.handleCallback(code, state));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        String username = UserContextHelper.currentUsername();
        Map<String, Object> res = new HashMap<>();
        authService.getConnection(username).ifPresentOrElse(
                (GoogleWorkspaceCredential c) -> {
                    res.put("connected", Boolean.TRUE.equals(c.getConnected()) && !Boolean.TRUE.equals(c.getRevoked()));
                    res.put("email", c.getEmail());
                    res.put("needsReconnect", Boolean.TRUE.equals(c.getRevoked()) || !Boolean.TRUE.equals(c.getConnected()));
                    res.put("connectedAt", c.getConnectedAt());
                    res.put("lastTokenRefreshAt", c.getLastTokenRefreshAt());
                    res.put("lastApiSuccessAt", c.getLastApiSuccessAt());

                    Map<String, Boolean> services = new HashMap<>();
                    services.put("gmail", c.hasScope("gmail.modify") || c.hasScope("gmail.send") || c.hasScope("gmail.readonly"));
                    services.put("calendar", c.hasScope("calendar"));
                    services.put("meet", c.hasScope("calendar"));
                    services.put("tasks", c.hasScope("tasks"));
                    res.put("services", services);
                },
                () -> res.put("connected", false)
        );
        return ResponseEntity.ok(res);
    }

    @PostMapping("/disconnect")
    public ResponseEntity<Void> disconnect() {
        authService.disconnect(UserContextHelper.currentUsername());
        return ResponseEntity.noContent().build();
    }
}
