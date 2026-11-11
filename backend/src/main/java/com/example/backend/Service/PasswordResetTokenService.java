package com.example.backend.Service;

import com.example.backend.Persistence.PasswordResetTokenEntity;
import com.example.backend.Repository.PasswordResetTokenRepository;
import com.example.backend.Repository.UtilisateurRepository;
import com.example.backend.Models.Utilisateur;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class PasswordResetTokenService {

    public static final Duration TOKEN_VALIDITY = Duration.ofMinutes(15);

    private static final int TOKEN_BYTES = 32;

    private final PasswordResetTokenRepository tokenRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public PasswordResetTokenService(PasswordResetTokenRepository tokenRepository, UtilisateurRepository utilisateurRepository) {
        this(tokenRepository, utilisateurRepository, Clock.systemUTC());
    }

    PasswordResetTokenService(PasswordResetTokenRepository tokenRepository, UtilisateurRepository utilisateurRepository, Clock clock) {
        this.tokenRepository = tokenRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.clock = clock;
    }

    /**
     * Crée un jeton brut destiné uniquement au futur lien d'email.
     * La base ne reçoit que son hash SHA-256.
     */
    @Transactional
    public String createToken(Integer utilisateurId) {
        if (utilisateurId == null) {
            throw new IllegalArgumentException("L'utilisateur est obligatoire.");
        }

        Utilisateur utilisateur = utilisateurRepository.findByIdForUpdate(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));

        Instant now = clock.instant();
        tokenRepository.invalidateUnusedTokens(utilisateurId, now);

        String rawToken = generateRawToken();

        PasswordResetTokenEntity entity = new PasswordResetTokenEntity();
        entity.setUtilisateurId(utilisateur.getId());
        entity.setTokenHash(hashToken(rawToken));
        entity.setCreatedAt(now);
        entity.setExpiresAt(now.plus(TOKEN_VALIDITY));
        tokenRepository.save(entity);

        return rawToken;
    }

    // findByTokenHash garde un verrou pessimiste ; cette lecture doit donc
    // s'exécuter dans une transaction d'écriture.
    @Transactional(readOnly = false)
    public boolean isValid(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }

        Instant now = clock.instant();
        return tokenRepository.findByTokenHash(hashToken(rawToken))
                .filter(token -> token.getUsedAt() == null)
                .filter(token -> token.getExpiresAt() != null && token.getExpiresAt().isAfter(now))
                .isPresent();
    }

    /**
     * Vérifie puis consomme atomiquement un jeton verrouillé en base.
     * Le service métier de changement de mot de passe pourra appeler cette
     * méthode dans sa propre transaction.
     */
    @Transactional
    public Optional<Integer> consume(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }

        Instant now = clock.instant();
        Optional<PasswordResetTokenEntity> token = tokenRepository.findByTokenHash(hashToken(rawToken))
                .filter(value -> value.getUsedAt() == null)
                .filter(value -> value.getExpiresAt() != null && value.getExpiresAt().isAfter(now));

        if (token.isEmpty()) {
            return Optional.empty();
        }

        PasswordResetTokenEntity entity = token.orElseThrow();
        entity.setUsedAt(now);
        tokenRepository.save(entity);
        return Optional.of(entity.getUtilisateurId());
    }

    String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("L'algorithme SHA-256 est indisponible.", exception);
        }
    }
}
