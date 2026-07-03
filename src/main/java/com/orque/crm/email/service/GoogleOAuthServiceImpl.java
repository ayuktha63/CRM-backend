package com.orque.crm.email.service;

import com.orque.crm.email.config.GoogleOAuthProperties;
import com.orque.crm.email.entity.ConnectedMailbox;
import com.orque.crm.email.repository.ConnectedMailboxRepository;
import com.orque.crm.enums.EmailProvider;
import com.orque.crm.enums.MailboxStatus;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthServiceImpl implements GoogleOAuthService {

    private final GoogleOAuthProperties properties;
    private final ConnectedMailboxRepository connectedMailboxRepository;

    @Override
    public String generateAuthorizationUrl() {

        return "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + properties.getClientId()
                + "&redirect_uri=" + properties.getRedirectUri()
                + "&response_type=code"
                + "&scope=" + properties.getScope()
                + "&access_type=offline"
                + "&prompt=consent";
    }

    @Override
    public String handleCallback(String code) {

        RestTemplate restTemplate = new RestTemplate();

        String tokenUrl =
                "https://oauth2.googleapis.com/token"
                        + "?code=" + code
                        + "&client_id=" + properties.getClientId()
                        + "&client_secret=" + properties.getClientSecret()
                        + "&redirect_uri=" + properties.getRedirectUri()
                        + "&grant_type=authorization_code";

        Map<String, Object> response =
                restTemplate.postForObject(
                        tokenUrl,
                        null,
                        Map.class
                );

        String accessToken =
                (String) response.get("access_token");

        String refreshToken =
                (String) response.get("refresh_token");

        Integer expiresIn =
                (Integer) response.get("expires_in");

        ConnectedMailbox mailbox =
                ConnectedMailbox.builder()
                        // TODO: Replace with authenticated Gmail user's email from Google UserInfo API
                        .emailAddress("gmail-user")
                        .provider(EmailProvider.GMAIL)
                        .status(MailboxStatus.CONNECTED)
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .tokenExpiresAt(
                                LocalDateTime.now()
                                        .plusSeconds(expiresIn)
                        )
                        .connectedAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

        connectedMailboxRepository.save(mailbox);

        return "Gmail connected successfully";
    }
}