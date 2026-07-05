package com.orque.crm.task.controller;

import com.orque.crm.common.UserContextHelper;
import com.orque.crm.task.entity.ConnectedGoogleCalendar;
import com.orque.crm.task.service.GoogleCalendarOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/calendar/google/oauth")
@RequiredArgsConstructor
public class GoogleCalendarOAuthController {

    private final GoogleCalendarOAuthService googleCalendarOAuthService;

    /** Returns the Google consent-screen URL for the current logged-in user to connect their own account. */
    @GetMapping("/url")
    public ResponseEntity<Map<String, String>> getAuthorizationUrl() {
        String username = UserContextHelper.currentUsername();
        return ResponseEntity.ok(Map.of("url", googleCalendarOAuthService.generateAuthorizationUrl(username)));
    }

    /** Google redirects the browser here directly (no auth header) — user identity comes from the signed state param. */
    @GetMapping("/callback")
    public RedirectView handleCallback(@RequestParam("code") String code, @RequestParam("state") String state) {
        return new RedirectView(googleCalendarOAuthService.handleCallback(code, state));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        String username = UserContextHelper.currentUsername();
        Map<String, Object> res = new HashMap<>();
        googleCalendarOAuthService.getConnection(username).ifPresentOrElse(
                (ConnectedGoogleCalendar c) -> {
                    res.put("connected", true);
                    res.put("googleEmail", c.getGoogleEmail());
                    res.put("syncEnabled", c.getSyncEnabled());
                    res.put("lastSyncedAt", c.getLastSyncedAt());
                },
                () -> res.put("connected", false)
        );
        return ResponseEntity.ok(res);
    }

    @PostMapping("/disconnect")
    public ResponseEntity<Void> disconnect() {
        googleCalendarOAuthService.disconnect(UserContextHelper.currentUsername());
        return ResponseEntity.noContent().build();
    }
}
