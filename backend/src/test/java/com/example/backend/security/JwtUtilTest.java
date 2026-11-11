package com.example.backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private static final String SECRET = "test-only-secret-with-more-than-32-bytes";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3_600_000L);
        jwtUtil.validateConfiguration();
    }

    @Test
    void ajouteLaVersionDAuthentificationSansModifierLeSujetMatricule() {
        String token = jwtUtil.generateToken("EMP-12", 4);

        assertEquals("EMP-12", jwtUtil.extractUsername(token));
        assertEquals(4, jwtUtil.extractAuthVersion(token));
        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    void considereUnJwtHistoriqueSansClaimCommeVersionZero() {
        String token = legacyToken("EMP-12");

        assertEquals("EMP-12", jwtUtil.extractUsername(token));
        assertEquals(0, jwtUtil.extractAuthVersion(token));
        assertTrue(jwtUtil.isTokenValid(token));
    }

    private String legacyToken(String matricule) {
        Date now = new Date();
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(matricule)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + 3_600_000L))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
