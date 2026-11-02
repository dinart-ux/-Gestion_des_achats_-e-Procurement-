package com.example.backend.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UtilisateurUpdateRequest(
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Size(max = 100) String prenom,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 30) String roleCode,
        @NotBlank String service,
        Integer managerId,
        @Size(max = 30) String telephone) {
}
