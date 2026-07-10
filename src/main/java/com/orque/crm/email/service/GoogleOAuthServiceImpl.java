package com.orque.crm.email.service;

import com.orque.crm.auth.entity.User;
import com.orque.crm.auth.repository.UserRepository;
import com.orque.crm.email.config.GoogleOAuthProperties;
import com.orque.crm.email.entity.ConnectedMailbox;
import com.orque.crm.email.repository.ConnectedMailboxRepository;
import com.orque.crm.enums.EmailProvider;
import com.orque.crm.enums.MailboxStatus;
import com.orque.crm.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthServiceImpl implements GoogleOAuthService {

    private final GoogleOAuthProperties properties;
    private final ConnectedMailboxRepository connectedMailboxRepository;
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
                + "&state=" + state;
    }

    @Override
    public String handleCallback(String code, String state) {
        String username = jwtService.extractUsernameFromStateToken(state);
        if (username == null) {
            return crmAppUrl + "/settings?googleEmail=error&reason=invalid_state";
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return crmAppUrl + "/settings?googleEmail=error&reason=unknown_user";
        }

        try {
            String tokenUrl = "https://oauth2.googleapis.com/token"
                    + "?code=" + encode(code)
                    + "&client_id=" + properties.getClientId()
                    + "&client_secret=" + properties.getClientSecret()
                    + "&redirect_uri=" + encode(properties.getRedirectUri())
                    + "&grant_type=authorization_code";

            @SuppressWarnings("unchecked")
            Map<String, Object> tokenResponse = restTemplate.postForObject(tokenUrl, null, Map.class);

            String accessToken = (String) tokenResponse.get("access_token");
            String refreshToken = (String) tokenResponse.get("refresh_token");
            Integer expiresIn = (Integer) tokenResponse.get("expires_in");

            String googleEmail = fetchGoogleEmail(accessToken);

            ConnectedMailbox mailbox = connectedMailboxRepository
                    .findByOwnerIgnoreCase(username)
                    .orElse(ConnectedMailbox.builder().owner(username).build());

            mailbox.setOrganizationId(user.getOrganizationId());
            mailbox.setEmailAddress(googleEmail);
            mailbox.setDisplayName(googleEmail);
            mailbox.setProvider(EmailProvider.GMAIL);
            mailbox.setStatus(MailboxStatus.CONNECTED);
            mailbox.setAccessToken(accessToken);
            // Google only returns a refresh_token on the very first consent; keep the old one otherwise.
            if (refreshToken != null && !refreshToken.isBlank()) {
                mailbox.setRefreshToken(refreshToken);
            }
            mailbox.setTokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn != null ? expiresIn : 3600));
            if (mailbox.getConnectedAt() == null) {
                mailbox.setConnectedAt(LocalDateTime.now());
            }
            mailbox.setUpdatedAt(LocalDateTime.now());

            connectedMailboxRepository.save(mailbox);

            return crmAppUrl + "/settings?googleEmail=connected";
        } catch (Exception e) {
            log.warn("Google email OAuth callback failed for user {}: {}", username, e.getMessage());
            return crmAppUrl + "/settings?googleEmail=error&reason=exchange_failed";
        }
    }

    private String fetchGoogleEmail(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = restTemplate.exchange(
                "https://www.googleapis.com/oauth2/v2/userinfo",
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        ).getBody();
        return userInfo != null ? (String) userInfo.get("email") : null;
    }

    @Override
    public Optional<ConnectedMailbox> getConnection(String username) {
        return connectedMailboxRepository.findByOwnerIgnoreCase(username);
    }

    @Override
    public void disconnect(String username) {
        connectedMailboxRepository.findByOwnerIgnoreCase(username)
                .ifPresent(mailbox -> {
                    mailbox.setStatus(MailboxStatus.DISCONNECTED);
                    mailbox.setUpdatedAt(LocalDateTime.now());
                    connectedMailboxRepository.save(mailbox);
                });
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
