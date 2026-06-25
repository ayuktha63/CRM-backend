package com.orque.crm.email.service;

public interface GoogleOAuthService {

    String generateAuthorizationUrl();

    String handleCallback(String code);
}
