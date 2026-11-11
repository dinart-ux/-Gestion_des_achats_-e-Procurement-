package com.example.backend.Service;

import com.example.backend.Models.Utilisateur;
import com.example.backend.Persistence.UtilisateurAuthStateEntity;
import com.example.backend.Repository.UtilisateurAuthStateRepository;
import com.example.backend.Repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UtilisateurAuthStateServiceTest {

    private final UtilisateurAuthStateRepository states = mock(UtilisateurAuthStateRepository.class);
    private final UtilisateurRepository utilisateurs = mock(UtilisateurRepository.class);
    private final UtilisateurAuthStateService service =
            new UtilisateurAuthStateService(states, utilisateurs);

    @Test
    void creeUneVersionZeroPourUnNouvelUtilisateur() {
        when(states.existsById(12)).thenReturn(false);

        service.ensureState(12);

        verify(states).save(any(UtilisateurAuthStateEntity.class));
    }

    @Test
    void incrementeLaVersionPourRevoquerLesAnciensJetons() {
        UtilisateurAuthStateEntity state = new UtilisateurAuthStateEntity();
        state.setUtilisateurId(12);
        state.setTokenVersion(3);
        when(states.incrementVersion(12)).thenAnswer(invocation -> {
            state.setTokenVersion(state.getTokenVersion() + 1);
            return 1;
        });
        when(states.findById(12)).thenReturn(Optional.of(state));

        assertEquals(4, service.incrementVersion(12));
        assertEquals(4, state.getTokenVersion());
        verify(states).incrementVersion(12);
    }

    @Test
    void refuseUnJwtSiLaLigneEtatEstAbsente() {
        Utilisateur user = new Utilisateur();
        user.setId(12);
        user.setMatricule("EMP-12");
        when(utilisateurs.findByMatricule("EMP-12")).thenReturn(Optional.of(user));
        when(states.findById(12)).thenReturn(Optional.empty());

        assertFalse(service.isCurrentVersion("EMP-12", 0));
    }

    @Test
    void accepteUnJwtQuandLaVersionStockeeCorrespond() {
        Utilisateur user = new Utilisateur();
        user.setId(12);
        user.setMatricule("EMP-12");
        UtilisateurAuthStateEntity state = new UtilisateurAuthStateEntity();
        state.setUtilisateurId(12);
        state.setTokenVersion(3);
        when(utilisateurs.findByMatricule("EMP-12")).thenReturn(Optional.of(user));
        when(states.findById(12)).thenReturn(Optional.of(state));

        assertTrue(service.isCurrentVersion("EMP-12", 3));
        assertFalse(service.isCurrentVersion("EMP-12", 0));
    }
}
