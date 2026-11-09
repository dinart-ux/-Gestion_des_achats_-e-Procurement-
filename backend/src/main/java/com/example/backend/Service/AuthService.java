package com.example.backend.Service;

import com.example.backend.DTO.LoginResponse;
import com.example.backend.Models.Utilisateur;
import com.example.backend.Repository.UtilisateurRepository;
import com.example.backend.Repository.RoleRepository;
import com.example.backend.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final JwtUtil jwtUtil;
    private final UtilisateurAuthStateService authStateService;

    public AuthService(AuthenticationManager authenticationManager,
                       UtilisateurRepository utilisateurRepository,
                       RoleRepository roleRepository,
                       JwtUtil jwtUtil,
                       UtilisateurAuthStateService authStateService) {
        this.authenticationManager = authenticationManager;
        this.utilisateurRepository = utilisateurRepository;
        this.roleRepository = roleRepository;
        this.jwtUtil = jwtUtil;
        this.authStateService = authStateService;
    }

    public LoginResponse connecter(String identifiant, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(identifiant, password)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        Utilisateur utilisateur = utilisateurRepository.findByMatricule(identifiant)
                .or(() -> utilisateurRepository.findByEmailIgnoreCase(identifiant))
                .orElseThrow(() -> new NoSuchElementException(
                        "Utilisateur non trouvé avec ce matricule ou cette adresse email."));

        // Génération du token en passant le matricule (String) à JwtUtil
        int authVersion = authStateService.ensureAndGetVersion(utilisateur.getId());
        String token = jwtUtil.generateToken(utilisateur.getMatricule(), authVersion);

        return new LoginResponse(
                "Connexion réussie",
                token,
                utilisateur.getMatricule(),
                utilisateur.getMatricule(),
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getEmail(),
                utilisateur.getRole() == null ? null
                        : roleRepository.findCodeById(utilisateur.getRole()).orElse(null)
        );
    }
}
