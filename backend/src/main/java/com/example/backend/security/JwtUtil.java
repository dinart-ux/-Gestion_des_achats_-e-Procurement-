package com.example.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    public static final String AUTH_VERSION_CLAIM = "authVersion";

    // La clé doit faire au minimum 32 octets pour HS256
    @Value("${jwt.secret:}")
    private String secret;

    @Value("${jwt.expiration-ms:3600000}")
    private long expirationMs;

    @PostConstruct
    void validateConfiguration() {
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET doit contenir au moins 32 octets.");
        }
        if (expirationMs <= 0) {
            throw new IllegalStateException("JWT_EXPIRATION_MS doit être strictement positif.");
        }
    }

    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username) {
        return generateToken(username, 0);
    }

    public String generateToken(String username, int authVersion) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(exp)
                .claim(AUTH_VERSION_CLAIM, authVersion)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Les JWT historiques ne possèdent pas ce claim et sont donc considérés
     * comme appartenant à la version 0.
     */
    public int extractAuthVersion(String token) {
        Object value = parseClaims(token).get(AUTH_VERSION_CLAIM);
        if (value == null) {
            return 0;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Claim authVersion invalide.", exception);
        }
    }

    private Claims parseClaims(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims;
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());

        } catch (Exception e) {
            return false;
        }
    }
}
