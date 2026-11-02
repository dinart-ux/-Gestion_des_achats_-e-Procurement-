package com.example.backend.DTO;

import jakarta.validation.constraints.NotBlank;

public record ChangeRoleRequest(@NotBlank(message = "Le rôle est obligatoire.") String roleCode) {
}