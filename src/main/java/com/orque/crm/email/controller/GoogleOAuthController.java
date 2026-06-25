package com.orque.crm.email.controller;

import com.orque.crm.email.service.GoogleOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/google/oauth")
@RequiredArgsConstructor
public class GoogleOAuthController {

    private final GoogleOAuthService googleOAuthService;

    @GetMapping("/url")
    public String getAuthorizationUrl() {
        return googleOAuthService.generateAuthorizationUrl();
    }

    @GetMapping("/callback")
    public String handleCallback(
            @RequestParam("code") String code
    ) {
        return googleOAuthService.handleCallback(code);
    }
}