package com.example.backend.Service;

import com.example.backend.Models.Utilisateur;
import com.example.backend.Persistence.UtilisateurAuthStateEntity;
import com.example.backend.Repository.UtilisateurAuthStateRepository;
import com.example.backend.Repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UtilisateurAuthStateService {

    private final UtilisateurAuthStateRepository stateRepository;
    private final UtilisateurRepository utilisateurRepository;

    public UtilisateurAuthStateService(
            UtilisateurAuthStateRepository stateRepository,
            UtilisateurRepository utilisateurRepository) {
        this.stateRepository = stateRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Transactional
    public void ensureState(Integer utilisateurId) {
        if (utilisateurId == null || stateRepository.existsById(utilisateurId)) {
            return;
        }

        UtilisateurAuthStateEntity state = new UtilisateurAuthStateEntity();
        state.setUtilisateurId(utilisateurId);
        state.setTokenVersion(0);
        stateRepository.save(state);
    }

    @Transactional
    public int ensureAndGetVersion(Integer utilisateurId) {
        if (utilisateurId == null) {
            return 0;
        }

        return stateRepository.findById(utilisateurId)
                .map(UtilisateurAuthStateEntity::getTokenVersion)
                .orElseGet(() -> {
                    ensureState(utilisateurId);
                    return 0;
                });
    }

    @Transactional
    public int incrementVersion(Integer utilisateurId) {
        if (utilisateurId == null) {
            throw new IllegalArgumentException("L'utilisateur est obligatoire.");
        }

        UtilisateurAuthStateEntity state = stateRepository.findById(utilisateurId)
                .orElseGet(() -> {
                    UtilisateurAuthStateEntity newState = new UtilisateurAuthStateEntity();
                    newState.setUtilisateurId(utilisateurId);
                    return newState;
                });

        state.setTokenVersion(Math.incrementExact(state.getTokenVersion()));
        stateRepository.save(state);
        return state.getTokenVersion();
    }

    @Transactional(readOnly = true)
    public boolean isCurrentVersion(String matricule, int tokenVersion) {
        if (matricule == null || matricule.isBlank() || tokenVersion < 0) {
            return false;
        }

        return utilisateurRepository.findByMatricule(matricule)
                .map(Utilisateur::getId)
                .map(stateRepository::findById)
                .map(state -> state.map(UtilisateurAuthStateEntity::getTokenVersion).orElse(0))
                .orElse(-1) == tokenVersion;
    }
}
