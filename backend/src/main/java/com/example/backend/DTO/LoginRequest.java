package com.example.backend.DTO;

import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonAlias;

public record LoginRequest(
        @JsonAlias({"matricule", "email"})
        @NotBlank(message = "Le matricule ou l'adresse email est obligatoire.") String identifiant,
        @NotBlank(message = "Le mot de passe est obligatoire.") String password) {
}
