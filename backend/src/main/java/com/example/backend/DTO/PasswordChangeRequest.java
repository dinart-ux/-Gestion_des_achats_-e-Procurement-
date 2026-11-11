package com.example.backend.DTO;

import jakarta.validation.constraints.NotBlank;

public record PasswordChangeRequest(@NotBlank String currentPassword, @NotBlank String newPassword,
                                    @NotBlank String confirmPassword) {}
