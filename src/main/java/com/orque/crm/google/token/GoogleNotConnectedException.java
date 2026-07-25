package com.orque.crm.google.token;

/** Thrown when a Google Workspace API is called for a user with no active, valid connection. */
public class GoogleNotConnectedException extends RuntimeException {
    public GoogleNotConnectedException(String message) {
        super(message);
    }
}
