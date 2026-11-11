package com.example.backend.Service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordResetRateLimiterTest {
    @Test
    void limiteChaqueCleACinqDemandesParFenetre() {
        PasswordResetRateLimiter limiter = new PasswordResetRateLimiter();
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.allow("user-and-ip"));
        }
        assertFalse(limiter.allow("user-and-ip"));
        assertTrue(limiter.allow("other-user-and-ip"));
    }
}
