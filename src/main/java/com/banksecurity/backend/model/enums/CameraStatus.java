package com.banksecurity.backend.model.enums;

public enum CameraStatus {
    ACTIVE("Caméra active"),
    INACTIVE("Caméra inactive"),
    ERROR("Erreur caméra"),
    MAINTENANCE("Maintenance");

    private final String description;

    CameraStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}