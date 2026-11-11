package com.example.backend.Service;

import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PasswordResetEmailServiceTest {
    @Test
    void envoieLeLienAuDestinataireAvecFrontendConnu() {
        JavaMailSender sender = mock(JavaMailSender.class);
        PasswordResetEmailService service = new PasswordResetEmailService(
                sender, "sender@example.test", "sender@example.test", "https://procurement.example.test/");

        service.sendResetLink("user@example.test", "opaque-token");

        var captor = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        assertEquals("user@example.test", captor.getValue().getTo()[0]);
        assertEquals("sender@example.test", captor.getValue().getFrom());
        org.junit.jupiter.api.Assertions.assertTrue(captor.getValue().getText()
                .contains("https://procurement.example.test/reset-password?token=opaque-token"));
    }

    @Test
    void propageProprementUneIndisponibiliteSmtp() {
        JavaMailSender sender = mock(JavaMailSender.class);
        doThrow(new org.springframework.mail.MailSendException("SMTP unavailable"))
                .when(sender).send(any(SimpleMailMessage.class));
        PasswordResetEmailService service = new PasswordResetEmailService(
                sender, "sender@example.test", "sender@example.test", "http://localhost:5173");

        assertThrows(org.springframework.mail.MailSendException.class,
                () -> service.sendResetLink("user@example.test", "opaque-token"));
    }

    @Test
    void refuseUnFrontendDistantEnHttp() {
        JavaMailSender sender = mock(JavaMailSender.class);
        assertThrows(IllegalArgumentException.class, () -> new PasswordResetEmailService(
                sender, "sender@example.test", "sender@example.test", "http://procurement.example.test"));
    }
}
