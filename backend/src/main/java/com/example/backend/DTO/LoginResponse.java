package com.example.backend.DTO;

public record LoginResponse(
        String message,
        String token,
        String id,
        String matricule,
        String nom,
        String prenom,
        String email,
        String role) {
}
