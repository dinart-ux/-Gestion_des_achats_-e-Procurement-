package com.example.backend.Service;

import com.example.backend.Models.Utilisateur;
import com.example.backend.Repository.UtilisateurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.BadCredentialsException;

@Service
public class PasswordResetService {
    private final PasswordResetTokenService tokens;
    private final UtilisateurRepository users;
    private final UtilisateurAuthStateService authState;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(PasswordResetTokenService tokens, UtilisateurRepository users,
                                UtilisateurAuthStateService authState, PasswordEncoder passwordEncoder) {
        this.tokens = tokens;
        this.users = users;
        this.authState = authState;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        Integer userId = tokens.consume(rawToken)
                .orElseThrow(() -> new IllegalArgumentException("Jeton invalide ou expiré."));
        Utilisateur user = users.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        user.setPassword(passwordEncoder.encode(newPassword));
        users.save(user);
        authState.incrementVersion(userId);
    }

    @Transactional
    public void changeOwnPassword(String matricule, String currentPassword, String newPassword) {
        Utilisateur user = users.findByMatricule(matricule)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadCredentialsException("Mot de passe actuel incorrect.");
        }
        user = users.findByIdForUpdate(user.getId()).orElseThrow();
        user.setPassword(passwordEncoder.encode(newPassword));
        users.save(user);
        authState.incrementVersion(user.getId());
    }
}
