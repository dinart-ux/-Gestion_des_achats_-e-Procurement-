package com.example.backend.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record PasswordForgotRequest(
        @NotBlank @Size(max = 20) String matricule,
        @NotBlank @Email @Size(max = 150) String email) {}
