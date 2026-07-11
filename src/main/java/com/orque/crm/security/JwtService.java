package com.orque.crm.security;

import com.orque.crm.auth.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Claims;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-expiration}")
    private long accessExpiration;

    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpiration;

    public String generateAccessToken(User user) {
        return generateToken(user, accessExpiration);
    }

    public String generateRefreshToken(User user) {
        return generateToken(user, refreshExpiration);
    }

    private String generateToken(User user, long expiration) {

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        SecretKey key = Keys.hmacShaKeyFor(
                jwtSecret.getBytes(StandardCharsets.UTF_8)
        );

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("userId", user.getId())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().getName().name())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /** Short-lived signed token used as the OAuth "state" param to prevent CSRF/account-hijack on callback. */
    public String generateOAuthStateToken(String username) {
        Date now = new Date();
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 600_000L)) // 10 minutes
                .signWith(key)
                .compact();
    }

    /** Verifies and decodes an OAuth state token; returns the username, or null if invalid/expired. */
    public String extractUsernameFromStateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            if (claims.getExpiration().before(new Date())) return null;
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isTokenValid(String token, User user) {
        String username = extractUsername(token);

        return username.equals(user.getUsername())
                && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        Date expirationDate = extractAllClaims(token).getExpiration();

        return expirationDate.before(new Date());
    }

    private Claims extractAllClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(
                jwtSecret.getBytes(StandardCharsets.UTF_8)
        );

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}