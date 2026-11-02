package com.example.backend.DTO;

import com.example.backend.Models.Utilisateur;

public record UtilisateurResponse(
        String id,
        String matricule,
        String nom,
        String prenom,
        String email,
        String role,
        String roleCode,
        String service,
        Integer managerId,
        boolean actif,
        String telephone) {

    public static UtilisateurResponse from(Utilisateur user, String roleCode) {
        return new UtilisateurResponse(
                user.getMatricule(), user.getMatricule(), user.getNom(), user.getPrenom(),
                user.getEmail(), user.getRole() != null ? user.getRole().toString() : null, roleCode,
                user.getService()!= null ? user.getService().toString() : null,
                user.getManagerId(),
                user.isActif(),
                user.getTelephone());
    }
}
