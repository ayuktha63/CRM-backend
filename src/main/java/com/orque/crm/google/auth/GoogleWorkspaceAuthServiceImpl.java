package com.orque.crm.google.auth;

import com.orque.crm.auth.entity.User;
import com.orque.crm.auth.repository.UserRepository;
import com.orque.crm.google.config.GoogleWorkspaceProperties;
import com.orque.crm.google.entity.GoogleWorkspaceCredential;
import com.orque.crm.google.repository.GoogleWorkspaceCredentialRepository;
import com.orque.crm.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleWorkspaceAuthServiceImpl implements GoogleWorkspaceAuthService {

    private final GoogleWorkspaceProperties properties;
    private final GoogleWorkspaceCredentialRepository credentialRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RestTemplate restTemplate;

    @Value("${crm.app-url}")
    private String crmAppUrl;

    @Override
    public String generateAuthorizationUrl(String username) {
        String state = jwtService.generateOAuthStateToken(username);
        return "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + properties.getClientId()
                + "&redirect_uri=" + encode(properties.getRedirectUri())
                + "&response_type=code"
                + "&scope=" + encode(properties.getScope())
                + "&access_type=offline"
                + "&prompt=consent"
                + "&include_granted_scopes=true"
                + "&state=" + state;
    }

    @Override
    public String handleCallback(String code, String state) {
        String username = jwtService.extractUsernameFromStateToken(state);
        if (username == null) {
            return crmAppUrl + "/settings?google=error&reason=invalid_state";
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return crmAppUrl + "/settings?google=error&reason=unknown_user";
        }

        try {
            // Google rejects confidential-client token requests that carry client_id/client_secret/code
            // as URL query params ("doesn't comply with Google's OAuth 2.0 policy") — must be a
            // form-urlencoded POST body.
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("code", code);
            form.add("client_id", properties.getClientId());
            form.add("client_secret", properties.getClientSecret());
            form.add("redirect_uri", properties.getRedirectUri());
            form.add("grant_type", "authorization_code");

            HttpHeaders formHeaders = new HttpHeaders();
            formHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            @SuppressWarnings("unchecked")
            Map<String, Object> tokenResponse = restTemplate.postForObject(
                    "https://oauth2.googleapis.com/token", new HttpEntity<>(form, formHeaders), Map.class);

            String accessToken = (String) tokenResponse.get("access_token");
            String refreshToken = (String) tokenResponse.get("refresh_token");
            Integer expiresIn = (Integer) tokenResponse.get("expires_in");
            String tokenType = (String) tokenResponse.get("token_type");
            String grantedScopes = (String) tokenResponse.get("scope");

            Map<String, Object> userInfo = fetchUserInfo(accessToken);
            String email = userInfo != null ? (String) userInfo.get("email") : null;
            String googleUserId = userInfo != null ? (String) userInfo.get("id") : null;

            GoogleWorkspaceCredential credential = credentialRepository
                    .findByOwnerIgnoreCase(username)
                    .orElse(GoogleWorkspaceCredential.builder().owner(username).build());

            credential.setOrganizationId(user.getOrganizationId());
            credential.setGoogleUserId(googleUserId);
            credential.setEmail(email);
            credential.setAccessToken(accessToken);
            // Google only returns a refresh_token on the very first consent; keep the old one otherwise.
            if (refreshToken != null && !refreshToken.isBlank()) {
                credential.setRefreshToken(refreshToken);
            }
            credential.setTokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn != null ? expiresIn : 3600));
            credential.setGrantedScopes(grantedScopes);
            credential.setTokenType(tokenType);
            credential.setConnected(true);
            credential.setRevoked(false);
            if (credential.getConnectedAt() == null) {
                credential.setConnectedAt(LocalDateTime.now());
            }
            credential.setUpdatedAt(LocalDateTime.now());

            credentialRepository.save(credential);

            return crmAppUrl + "/settings?google=connected";
        } catch (Exception e) {
            log.warn("Google Workspace OAuth callback failed for user {}: {}", username, e.getMessage());
            return crmAppUrl + "/settings?google=error&reason=exchange_failed";
        }
    }

    private Map<String, Object> fetchUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = restTemplate.exchange(
                "https://www.googleapis.com/oauth2/v2/userinfo",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        ).getBody();
        return userInfo;
    }

    @Override
    public Optional<GoogleWorkspaceCredential> getConnection(String username) {
        return credentialRepository.findByOwnerIgnoreCase(username);
    }

    @Override
    public void disconnect(String username) {
        credentialRepository.findByOwnerIgnoreCase(username)
                .ifPresent(credential -> {
                    revokeAtGoogle(credential.getAccessToken());
                    credentialRepository.delete(credential);
                });
    }

    private void revokeAtGoogle(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) return;
        try {
            restTemplate.postForObject(
                    "https://oauth2.googleapis.com/revoke?token=" + encode(accessToken), null, Void.class);
        } catch (Exception e) {
            log.info("Google token revoke call failed (token may already be invalid): {}", e.getMessage());
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
