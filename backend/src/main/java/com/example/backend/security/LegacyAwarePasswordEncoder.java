package com.example.backend.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Accepts legacy plaintext credentials once, then Spring upgrades them to BCrypt. */
public final class LegacyAwarePasswordEncoder implements PasswordEncoder {
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
        return bcrypt.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String storedPassword) {
        if (storedPassword == null) return false;
        if (isBcrypt(storedPassword)) return bcrypt.matches(rawPassword, storedPassword);
        return MessageDigest.isEqual(rawPassword.toString().getBytes(StandardCharsets.UTF_8),
                storedPassword.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean upgradeEncoding(String storedPassword) {
        return storedPassword == null || !isBcrypt(storedPassword) || bcrypt.upgradeEncoding(storedPassword);
    }

    private boolean isBcrypt(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }
}
