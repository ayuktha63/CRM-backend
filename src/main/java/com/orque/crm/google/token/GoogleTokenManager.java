package com.orque.crm.google.token;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.orque.crm.google.config.GoogleWorkspaceProperties;
import com.orque.crm.google.entity.GoogleWorkspaceCredential;
import com.orque.crm.google.repository.GoogleWorkspaceCredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Single point of contact for turning a stored, encrypted Google credential into a live,
 * auto-refreshing API client. Every google/* service (Gmail, Calendar, Tasks) goes through
 * here rather than touching {@link GoogleWorkspaceCredentialRepository} or token refresh
 * logic directly.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleTokenManager {

    private final GoogleWorkspaceCredentialRepository credentialRepository;
    private final GoogleWorkspaceProperties properties;

    /** Loads the user's connection, throwing if there isn't one or it's been revoked. */
    public GoogleWorkspaceCredential requireConnected(String username) {
        GoogleWorkspaceCredential credential = credentialRepository.findByOwnerIgnoreCase(username)
                .orElseThrow(() -> new GoogleNotConnectedException("Google Workspace is not connected for this user."));
        if (!Boolean.TRUE.equals(credential.getConnected()) || Boolean.TRUE.equals(credential.getRevoked())) {
            throw new GoogleNotConnectedException("Google Workspace access was revoked or disconnected — please reconnect.");
        }
        return credential;
    }

    /**
     * Builds a {@link GoogleCredential} that transparently refreshes the access token on demand
     * (the underlying google-api-client library retries once on a 401 using the refresh token).
     * Any resulting new access token is persisted back so the stored copy stays current.
     */
    public GoogleCredential buildCredential(GoogleWorkspaceCredential credential) {
        try {
            String username = credential.getOwner();
            GoogleCredential.Builder builder = new GoogleCredential.Builder()
                    .setTransport(GoogleNetHttpTransport.newTrustedTransport())
                    .setJsonFactory(GsonFactory.getDefaultInstance())
                    .setClientSecrets(properties.getClientId(), properties.getClientSecret())
                    .addRefreshListener(new com.google.api.client.auth.oauth2.CredentialRefreshListener() {
                        @Override
                        public void onTokenResponse(com.google.api.client.auth.oauth2.Credential cred,
                                                     com.google.api.client.auth.oauth2.TokenResponse tokenResponse) {
                            persistRefreshedToken(username, cred.getAccessToken());
                        }

                        @Override
                        public void onTokenErrorResponse(com.google.api.client.auth.oauth2.Credential cred,
                                                          com.google.api.client.auth.oauth2.TokenErrorResponse errorResponse) {
                            log.warn("Token refresh failed for user {}: {}", username,
                                    errorResponse != null ? errorResponse.getError() : "unknown");
                        }
                    });

            GoogleCredential googleCredential = builder.build();
            googleCredential.setAccessToken(credential.getAccessToken());
            googleCredential.setRefreshToken(credential.getRefreshToken());
            return googleCredential;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build Google API client", e);
        }
    }

    private void persistRefreshedToken(String username, String newAccessToken) {
        credentialRepository.findByOwnerIgnoreCase(username).ifPresent(c -> {
            c.setAccessToken(newAccessToken);
            c.setLastTokenRefreshAt(LocalDateTime.now());
            credentialRepository.save(c);
        });
    }

    /** Call after any successful Google API call so the Integrations page can show real freshness. */
    public void recordApiSuccess(String username) {
        credentialRepository.findByOwnerIgnoreCase(username).ifPresent(c -> {
            c.setLastApiSuccessAt(LocalDateTime.now());
            credentialRepository.save(c);
        });
    }

    /**
     * Call from a catch block around any Google API call. If the failure means the refresh token
     * itself is dead, flips the connection to disconnected/revoked so the UI stops silently
     * retrying and prompts the user to reconnect instead. Returns true if it was in fact revoked.
     */
    public boolean recordIfRevoked(String username, Throwable e) {
        if (!GoogleAuthErrorClassifier.isRevoked(e)) {
            return false;
        }
        credentialRepository.findByOwnerIgnoreCase(username).ifPresent(c -> {
            if (Boolean.TRUE.equals(c.getConnected())) {
                c.setConnected(false);
                c.setRevoked(true);
                c.setUpdatedAt(LocalDateTime.now());
                credentialRepository.save(c);
                log.warn("Google Workspace access revoked for user {} — disconnecting until reconnect", username);
            }
        });
        return true;
    }
}
