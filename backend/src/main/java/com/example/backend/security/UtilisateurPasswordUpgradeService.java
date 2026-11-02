package com.example.backend.security;

import com.example.backend.Models.Utilisateur;
import com.example.backend.Repository.UtilisateurRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.stereotype.Service;

@Service
public class UtilisateurPasswordUpgradeService implements UserDetailsPasswordService {

    private final UtilisateurRepository utilisateurRepository;

    public UtilisateurPasswordUpgradeService(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    public UserDetails updatePassword(UserDetails user, String newPassword) {
        // Recherche par matricule (getUsername() renvoie le matricule maintenant)
        Utilisateur utilisateur = utilisateurRepository.findByMatricule(user.getUsername()).orElse(null);
        if (utilisateur != null) {
            utilisateur.setPassword(newPassword);
            utilisateurRepository.save(utilisateur);
        }
        return user;
    }
}