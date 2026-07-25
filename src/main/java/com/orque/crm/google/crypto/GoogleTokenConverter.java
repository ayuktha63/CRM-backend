package com.orque.crm.google.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encrypts Google OAuth access/refresh tokens at rest using AES/GCM so they are never
 * stored or logged in plaintext. Applied via {@code @Convert} on entity token fields.
 *
 * <p>JPA instantiates {@code AttributeConverter}s directly (not through the Spring context),
 * so the encryption key is read from the environment rather than injected — keep
 * GOOGLE_TOKEN_ENCRYPTION_KEY set to the same 32+ char secret in every environment; losing it
 * makes every stored token permanently undecryptable.
 */
@Converter
public class GoogleTokenConverter implements AttributeConverter<String, String> {

    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String DEFAULT_SECRET = "orque-google-token-default-key-change-me";

    private String secret() {
        String fromEnv = System.getenv("GOOGLE_TOKEN_ENCRYPTION_KEY");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        String fromProp = System.getProperty("google.token-encryption-key");
        return (fromProp != null && !fromProp.isBlank()) ? fromProp : DEFAULT_SECRET;
    }

    @Override
    public String convertToDatabaseColumn(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return plainText;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt Google OAuth token", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbValue) {
        if (dbValue == null || dbValue.isBlank()) {
            return dbValue;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(dbValue);
            byte[] iv = new byte[IV_BYTES];
            System.arraycopy(combined, 0, iv, 0, IV_BYTES);
            byte[] cipherText = new byte[combined.length - IV_BYTES];
            System.arraycopy(combined, IV_BYTES, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt Google OAuth token", e);
        }
    }

    private SecretKeySpec keySpec() throws Exception {
        // Derive a 256-bit key from the configured secret so any secret length is accepted.
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret().getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(digest, "AES");
    }
}
