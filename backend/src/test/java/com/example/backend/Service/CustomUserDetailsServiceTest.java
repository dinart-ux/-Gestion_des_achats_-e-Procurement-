package com.example.backend.Service;

import com.example.backend.Models.Utilisateur;
import com.example.backend.Repository.RoleRepository;
import com.example.backend.Repository.UtilisateurRepository;
import com.example.backend.security.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTest {

    private final UtilisateurRepository utilisateurs = mock(UtilisateurRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final CustomUserDetailsService service = new CustomUserDetailsService(utilisateurs, roles);

    @Test
    void chargeParMatriculeEtUtiliseLeCodeRoleIndependammentDeSonId() {
        Utilisateur utilisateur = utilisateur(83, "EMP-83", 917, true);
        when(utilisateurs.findByMatricule("EMP-83")).thenReturn(Optional.of(utilisateur));
        when(roles.findCodeById(917)).thenReturn(Optional.of("ADMIN"));

        UserDetails details = service.loadUserByUsername("EMP-83");

        assertEquals("EMP-83", details.getUsername());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(details.isEnabled());
    }

    @Test
    void accepteEmailCommeIdentifiantMaisGardeLeMatriculeCommeNomDuPrincipal() {
        Utilisateur utilisateur = utilisateur(86, "EMP-86", 903, true);
        when(utilisateurs.findByMatricule("personne@example.test")).thenReturn(Optional.empty());
        when(utilisateurs.findByEmailIgnoreCase("personne@example.test")).thenReturn(Optional.of(utilisateur));
        when(roles.findCodeById(903)).thenReturn(Optional.of("MANAGER"));

        UserDetails details = service.loadUserByUsername("personne@example.test");

        assertEquals("EMP-86", details.getUsername());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER")));
    }

    @Test
    void conserveLeBlocageDesComptesInactifs() {
        Utilisateur utilisateur = utilisateur(84, "EMP-84", 902, false);
        when(utilisateurs.findByMatricule("EMP-84")).thenReturn(Optional.of(utilisateur));
        when(roles.findCodeById(902)).thenReturn(Optional.of("DEMANDEUR"));

        assertFalse(service.loadUserByUsername("EMP-84").isEnabled());
    }

    @Test
    void refuseUnRoleAbsentDeLaTable() {
        Utilisateur utilisateur = utilisateur(85, "EMP-85", 999, true);
        when(utilisateurs.findByMatricule("EMP-85")).thenReturn(Optional.of(utilisateur));
        when(roles.findCodeById(999)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("EMP-85"));
    }

    private Utilisateur utilisateur(int id, String matricule, int roleId, boolean actif) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(id);
        utilisateur.setMatricule(matricule);
        utilisateur.setRole(roleId);
        utilisateur.setPassword("encoded-password");
        utilisateur.setActif(actif);
        return utilisateur;
    }
}
