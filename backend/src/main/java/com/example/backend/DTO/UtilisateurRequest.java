package com.example.backend.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UtilisateurRequest(
        @NotBlank @Size(max = 20) String matricule,
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Size(max = 100) String prenom,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 30) String roleCode,
        @NotBlank String service,
        Integer managerId,
        @Size(max = 30) String telephone) {
}
