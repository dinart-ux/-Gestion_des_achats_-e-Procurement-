package com.example.backend.Service;

import com.example.backend.DTO.LoginResponse;
import com.example.backend.Models.Utilisateur;
import com.example.backend.Repository.UtilisateurRepository;
import com.example.backend.Repository.RoleRepository;
import com.example.backend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testConnecterSuccess() {
        // Arrange
        String matricule = "EMP001";
        String password = "2025";

        Utilisateur mockUtilisateur = new Utilisateur();
        mockUtilisateur.setMatricule(matricule);
        mockUtilisateur.setId(741);
        mockUtilisateur.setNom("Jean");
        mockUtilisateur.setPrenom("Dupont");
        mockUtilisateur.setEmail("jean.dupont@example.com");
        mockUtilisateur.setRole(1); // Passage d'un Integer au lieu d'un String pour correspondre à PostgreSQL

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(utilisateurRepository.findByMatricule(matricule))
                .thenReturn(Optional.of(mockUtilisateur));
        when(jwtUtil.generateToken(matricule)).thenReturn("test-token");
        when(roleRepository.findCodeById(1)).thenReturn(Optional.of("DEMANDEUR"));

        // Act
        LoginResponse response = authService.connecter(matricule, password);

        // Assert
        assertNotNull(response);
        assertEquals("test-token", response.token());
        assertEquals(matricule, response.matricule());
        assertEquals(matricule, response.id());
        assertEquals("Jean", response.nom());
        assertEquals("Dupont", response.prenom());
        assertEquals("DEMANDEUR", response.role());
        assertEquals("Connexion réussie", response.message());

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(utilisateurRepository, times(1)).findByMatricule(matricule);
    }

    @Test
    void accepteUneAdresseEmailEtEmetUnJetonLieAuMatricule() {
        String email = "jean.dupont@example.com";
        String matricule = "EMP002";
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setMatricule(matricule);
        utilisateur.setEmail(email);
        utilisateur.setRole(2);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(utilisateurRepository.findByMatricule(email)).thenReturn(Optional.empty());
        when(utilisateurRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(utilisateur));
        when(jwtUtil.generateToken(matricule)).thenReturn("email-login-token");
        when(roleRepository.findCodeById(2)).thenReturn(Optional.of("MANAGER"));

        LoginResponse response = authService.connecter(email, "password-not-reported");

        assertEquals("email-login-token", response.token());
        assertEquals(matricule, response.matricule());
        verify(utilisateurRepository).findByEmailIgnoreCase(email);
        verify(jwtUtil).generateToken(matricule);
    }
}
