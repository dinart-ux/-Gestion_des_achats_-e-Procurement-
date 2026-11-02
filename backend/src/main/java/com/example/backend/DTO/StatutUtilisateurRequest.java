
package com.example.backend.DTO;

import jakarta.validation.constraints.NotNull;

public record StatutUtilisateurRequest(
        @NotNull Boolean actif
) {
}
