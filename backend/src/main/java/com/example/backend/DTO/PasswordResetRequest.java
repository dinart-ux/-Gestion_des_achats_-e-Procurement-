package com.example.backend.DTO;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(@NotBlank String token, @NotBlank String newPassword,
                                   @NotBlank String confirmPassword) {}
