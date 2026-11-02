package com.example.backend.security;

import com.example.backend.Models.Utilisateur;
import com.example.backend.Repository.RoleRepository;
import com.example.backend.Repository.UtilisateurRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;

    public CustomUserDetailsService(UtilisateurRepository utilisateurRepository,
                                    RoleRepository roleRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifiant) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findByMatricule(identifiant)
                .or(() -> utilisateurRepository.findByEmailIgnoreCase(identifiant))
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur non trouvé avec ce matricule ou cette adresse email."));

        if (utilisateur.getMatricule() == null || utilisateur.getMatricule().isBlank()) {
            throw new UsernameNotFoundException(
                    "Le compte ne possède pas de matricule utilisable pour l'authentification.");
        }

        Integer roleId = utilisateur.getRole();
        if (roleId == null) {
            throw new UsernameNotFoundException("Aucun rôle associé au compte.");
        }

        String roleCode = roleRepository.findCodeById(roleId)
                .filter(code -> !code.isBlank())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Rôle introuvable pour le compte."));
        String authority = roleCode.startsWith("ROLE_") ? roleCode : "ROLE_" + roleCode;

        return User.withUsername(utilisateur.getMatricule())
                .password(utilisateur.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority(authority)))
                .disabled(!utilisateur.isActif())
                .build();
    }
}
