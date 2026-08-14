package com.banksecurity.backend.model.enums;

public enum AlertStatus {
    PENDING("En attente"),
    PROCESSING("En cours de traitement"),
    RESOLVED("Résolue"),
    FALSE_ALARM("Fausse alerte"),
    ESCALATED("Escaladée");

    private final String description;

    AlertStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}