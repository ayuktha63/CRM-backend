package com.orque.crm.email.controller;

import com.orque.crm.common.UserContextHelper;
import com.orque.crm.email.entity.ConnectedMailbox;
import com.orque.crm.email.service.GoogleOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/email/google/oauth")
@RequiredArgsConstructor
public class GoogleOAuthController {

    private final GoogleOAuthService googleOAuthService;

    /** Returns the Google consent-screen URL for the current logged-in user to connect their own Gmail. */
    @GetMapping("/url")
    public ResponseEntity<Map<String, String>> getAuthorizationUrl() {
        String username = UserContextHelper.currentUsername();
        return ResponseEntity.ok(Map.of("url", googleOAuthService.generateAuthorizationUrl(username)));
    }

    /** Google redirects the browser here directly (no auth header) — user identity comes from the signed state param. */
    @GetMapping("/callback")
    public RedirectView handleCallback(@RequestParam("code") String code, @RequestParam("state") String state) {
        return new RedirectView(googleOAuthService.handleCallback(code, state));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        String username = UserContextHelper.currentUsername();
        Map<String, Object> res = new HashMap<>();
        googleOAuthService.getConnection(username).ifPresentOrElse(
                (ConnectedMailbox m) -> {
                    res.put("connected", m.getStatus() == com.orque.crm.enums.MailboxStatus.CONNECTED);
                    res.put("googleEmail", m.getEmailAddress());
                },
                () -> res.put("connected", false)
        );
        return ResponseEntity.ok(res);
    }

    @PostMapping("/disconnect")
    public ResponseEntity<Void> disconnect() {
        googleOAuthService.disconnect(UserContextHelper.currentUsername());
        return ResponseEntity.noContent().build();
    }
}
