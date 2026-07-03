package com.orque.crm.license.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class LicenseEncryptionUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Value("${crm.license.secret:orque-licensing-secret-key-32ch!}")
    private String secret;

    @Value("${crm.license.ivHex:1234567890abcdef1234567890abcdef}")
    private String ivHex;

    /** Decrypt a HEX-formatted license key and return the parsed OpacLicensePayload. */
    public OpacLicensePayload decryptHex(String encryptedHex) {
        try {
            String hex = encryptedHex.replaceAll("[\\s\\-]", "").trim();
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            byte[] ivBytes = hexStringToByteArray(ivHex);
            byte[] encryptedBytes = hexStringToByteArray(hex);

            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            String json = new String(decryptedBytes, StandardCharsets.UTF_8);
            return MAPPER.readValue(json, OpacLicensePayload.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid license key: " + e.getMessage(), e);
        }
    }

    /** Encrypt a payload into a HEX string. */
    public String encryptHex(String payload) {
        try {
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            byte[] ivBytes = hexStringToByteArray(ivHex);

            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] encryptedBytes = cipher.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return byteArrayToHexString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt license", e);
        }
    }

    /** HmacSHA256 Digital Signature generation */
    public String generateHmac(String data) throws Exception {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(signingKey);
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return byteArrayToHexString(rawHmac);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private static String byteArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Payload DTOs (OPAC Structure)
    // ──────────────────────────────────────────────────────────────────────────

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class OpacLicensePayload {
        private String licenseVersion;
        private String issueDate;
        private String expiryDate;
        private String licenseType;
        private OpacTenant tenant;
        private List<OpacProduct> products;
        private String digitalSignature;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class OpacTenant {
        private String tenantName;
        private String company;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class OpacProduct {
        private String productName;
        private String startDate;
        private String endDate;
        private int userLimit;
        private int concurrentLimit;
        private int gracePeriod;
        private List<String> features;
    }
}
