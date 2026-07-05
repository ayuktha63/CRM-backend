package com.orque.crm.task.service;

import com.orque.crm.auth.entity.User;
import com.orque.crm.auth.repository.UserRepository;
import com.orque.crm.security.JwtService;
import com.orque.crm.task.config.GoogleCalendarOAuthProperties;
import com.orque.crm.task.entity.ConnectedGoogleCalendar;
import com.orque.crm.task.repository.ConnectedGoogleCalendarRepository;
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
public class GoogleCalendarOAuthServiceImpl implements GoogleCalendarOAuthService {

    private final GoogleCalendarOAuthProperties properties;
    private final ConnectedGoogleCalendarRepository connectedGoogleCalendarRepository;
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
            return crmAppUrl + "/settings?googleCalendar=error&reason=invalid_state";
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return crmAppUrl + "/settings?googleCalendar=error&reason=unknown_user";
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

            ConnectedGoogleCalendar connection = connectedGoogleCalendarRepository
                    .findByOwnerIgnoreCase(username)
                    .orElse(ConnectedGoogleCalendar.builder().owner(username).build());

            connection.setOrganizationId(user.getOrganizationId());
            connection.setGoogleEmail(googleEmail);
            connection.setAccessToken(accessToken);
            // Google only returns a refresh_token on the very first consent; keep the old one otherwise.
            if (refreshToken != null && !refreshToken.isBlank()) {
                connection.setRefreshToken(refreshToken);
            }
            connection.setTokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn != null ? expiresIn : 3600));
            connection.setSyncEnabled(true);
            if (connection.getConnectedAt() == null) {
                connection.setConnectedAt(LocalDateTime.now());
            }
            connection.setUpdatedAt(LocalDateTime.now());

            connectedGoogleCalendarRepository.save(connection);

            return crmAppUrl + "/settings?googleCalendar=connected";
        } catch (Exception e) {
            log.warn("Google Calendar OAuth callback failed for user {}: {}", username, e.getMessage());
            return crmAppUrl + "/settings?googleCalendar=error&reason=exchange_failed";
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
    public Optional<ConnectedGoogleCalendar> getConnection(String username) {
        return connectedGoogleCalendarRepository.findByOwnerIgnoreCase(username);
    }

    @Override
    public void disconnect(String username) {
        connectedGoogleCalendarRepository.findByOwnerIgnoreCase(username)
                .ifPresent(connectedGoogleCalendarRepository::delete);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
