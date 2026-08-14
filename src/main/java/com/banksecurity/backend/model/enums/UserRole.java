package com.banksecurity.backend.model.enums;

public enum UserRole {
    ADMIN("Administrateur système"),
    SECURITY("Agent de sécurité"),
    MANAGER("Responsable sécurité");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}