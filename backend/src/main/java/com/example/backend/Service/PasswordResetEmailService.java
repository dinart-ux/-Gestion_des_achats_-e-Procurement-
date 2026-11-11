package com.example.backend.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetEmailService {
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetEmailService.class);
    private final JavaMailSender sender;
    private final String from;
    private final String frontendUrl;

    public PasswordResetEmailService(JavaMailSender sender,
            @Value("${spring.mail.username:}") String configuredUsername,
            @Value("${app.mail.from:}") String configuredFrom,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.sender = sender;
        this.from = configuredFrom.isBlank() ? configuredUsername : configuredFrom;
        this.frontendUrl = validateFrontendUrl(frontendUrl);
    }

    public void sendResetLink(String destination, String rawToken) {
        if (from == null || from.isBlank() || frontendUrl.isBlank()) {
            throw new IllegalStateException("Le service de courrier n'est pas configuré.");
        }
        String link = frontendUrl + "/reset-password?token="
                + java.net.URLEncoder.encode(rawToken, java.nio.charset.StandardCharsets.UTF_8);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(destination);
        message.setSubject("Réinitialisation de votre mot de passe e-Procurement");
        message.setText("Pour choisir un nouveau mot de passe, ouvrez ce lien (valide 15 minutes) :\n\n" + link);
        try {
            sender.send(message);
        } catch (MailException exception) {
            // Ne jamais journaliser l'exception elle-même : certaines implémentations peuvent inclure
            // des détails du message. Le type suffit au diagnostic sans exposer adresse ou jeton.
            logger.warn("Échec de l'envoi du courriel de récupération via SMTP ({}).",
                    exception.getClass().getSimpleName());
            throw exception;
        }
    }

    private String validateFrontendUrl(String value) {
        if (value == null || value.isBlank()) return "";
        java.net.URI uri;
        try {
            uri = java.net.URI.create(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("APP_FRONTEND_URL doit être une URL valide.");
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        boolean local = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host);
        if (host == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                || !("https".equalsIgnoreCase(scheme) || (local && "http".equalsIgnoreCase(scheme)))) {
            throw new IllegalArgumentException("APP_FRONTEND_URL doit utiliser HTTPS hors environnement local.");
        }
        return uri.toString().replaceAll("/+$", "");
    }
}
