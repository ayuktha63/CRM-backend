package com.orque.crm.google.auth;

import com.orque.crm.google.entity.GoogleWorkspaceCredential;

import java.util.Optional;

public interface GoogleWorkspaceAuthService {

    /** Builds the Google consent-screen URL for the given user, embedding a signed CSRF state token. */
    String generateAuthorizationUrl(String username);

    /**
     * Exchanges the authorization code for tokens, verifies the state token, and persists the
     * single Google Workspace connection for the user encoded in the state. Returns the frontend
     * URL to redirect the browser back to.
     */
    String handleCallback(String code, String state);

    Optional<GoogleWorkspaceCredential> getConnection(String username);

    /** Best-effort revokes the token at Google, then removes the local connection. */
    void disconnect(String username);
}
