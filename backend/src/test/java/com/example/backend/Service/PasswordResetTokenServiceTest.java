package com.example.backend.Service;

import com.example.backend.Persistence.PasswordResetTokenEntity;
import com.example.backend.Repository.PasswordResetTokenRepository;
import com.example.backend.Repository.UtilisateurRepository;
import com.example.backend.Models.Utilisateur;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-09T10:00:00Z");

    private PasswordResetTokenRepository repository;
    private UtilisateurRepository users;
    private PasswordResetTokenService service;

    @BeforeEach
    void setUp() {
        repository = mock(PasswordResetTokenRepository.class);
        users = mock(UtilisateurRepository.class);
        service = new PasswordResetTokenService(
                repository,
                users,
                Clock.fixed(NOW, ZoneOffset.UTC));
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(12);
        when(users.findByIdForUpdate(12)).thenReturn(Optional.of(utilisateur));
        when(repository.save(any(PasswordResetTokenEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void genereUnJetonDontSeulLeHashEstPersistéAvecUneDureeDeQuinzeMinutes() {
        String rawToken = service.createToken(12);

        ArgumentCaptor<PasswordResetTokenEntity> captor =
                ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        verify(repository).save(captor.capture());

        PasswordResetTokenEntity persisted = captor.getValue();
        assertNotEquals(rawToken, persisted.getTokenHash());
        assertEquals(64, persisted.getTokenHash().length());
        assertEquals(NOW, persisted.getCreatedAt());
        assertEquals(NOW.plus(PasswordResetTokenService.TOKEN_VALIDITY), persisted.getExpiresAt());
        verify(repository).invalidateUnusedTokens(12, NOW);
    }

    @Test
    void valideUnJetonNonUtiliseEtNonExpire() {
        String rawToken = service.createToken(12);
        ArgumentCaptor<PasswordResetTokenEntity> captor =
                ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        verify(repository).save(captor.capture());
        PasswordResetTokenEntity persisted = captor.getValue();

        when(repository.findByTokenHash(persisted.getTokenHash()))
                .thenReturn(Optional.of(persisted));

        assertTrue(service.isValid(rawToken));
    }

    @Test
    void refuseUnJetonExpireOuDejaUtilise() {
        String rawToken = service.createToken(12);
        ArgumentCaptor<PasswordResetTokenEntity> captor =
                ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        verify(repository).save(captor.capture());
        PasswordResetTokenEntity persisted = captor.getValue();
        when(repository.findByTokenHash(persisted.getTokenHash()))
                .thenReturn(Optional.of(persisted));

        persisted.setExpiresAt(NOW.minusSeconds(1));
        assertFalse(service.isValid(rawToken));

        persisted.setExpiresAt(NOW.plusSeconds(1));
        persisted.setUsedAt(NOW);
        assertFalse(service.isValid(rawToken));
    }

    @Test
    void consumeVerrouilleEtMarqueLeJetonCommeUtilise() {
        String rawToken = service.createToken(12);
        ArgumentCaptor<PasswordResetTokenEntity> captor =
                ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        verify(repository).save(captor.capture());
        PasswordResetTokenEntity persisted = captor.getValue();
        when(repository.findByTokenHash(persisted.getTokenHash()))
                .thenReturn(Optional.of(persisted));

        assertEquals(Optional.of(12), service.consume(rawToken));
        assertEquals(NOW, persisted.getUsedAt());
        assertEquals(Optional.empty(), service.consume(rawToken));
    }
}
