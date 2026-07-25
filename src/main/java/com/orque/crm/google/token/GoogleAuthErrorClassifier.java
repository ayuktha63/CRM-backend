package com.orque.crm.google.token;

/**
 * Distinguishes "the user revoked/expired their Google refresh token" (unrecoverable without a
 * fresh consent flow) from transient failures (network blip, quota, Google downtime) so callers
 * know when to stop retrying and flag the connection for reconnect instead.
 */
public final class GoogleAuthErrorClassifier {

    private GoogleAuthErrorClassifier() {}

    public static boolean isRevoked(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String message = t.getMessage();
            if (message != null && (message.contains("invalid_grant")
                    || message.contains("Token has been expired or revoked")
                    || message.contains("unauthorized_client"))) {
                return true;
            }
            if (t instanceof com.google.api.client.auth.oauth2.TokenResponseException tre
                    && tre.getDetails() != null
                    && "invalid_grant".equals(tre.getDetails().getError())) {
                return true;
            }
        }
        return false;
    }
}
