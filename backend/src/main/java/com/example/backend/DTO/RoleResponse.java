package com.example.backend.DTO;

public record RoleResponse(String id, String code, String libelle) {
    public static RoleResponse from(String code) {
        return new RoleResponse(code, code, code);
    }
}
