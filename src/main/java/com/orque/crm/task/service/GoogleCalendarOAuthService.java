package com.orque.crm.task.service;

import com.orque.crm.task.entity.ConnectedGoogleCalendar;

import java.util.Optional;

public interface GoogleCalendarOAuthService {

    /** Builds the Google consent-screen URL for the current user, embedding a signed CSRF state token. */
    String generateAuthorizationUrl(String username);

    /**
     * Exchanges the authorization code for tokens, verifies the state token, and persists the
     * connection for the user encoded in the state. Returns the frontend URL to redirect the
     * browser back to.
     */
    String handleCallback(String code, String state);

    Optional<ConnectedGoogleCalendar> getConnection(String username);

    void disconnect(String username);
}
