package com.banksecurity.backend.model.enums;

public enum ZoneType {
    DAB("Distributeur Automatique de Billets"),
    BACK_OFFICE("Zone réservée au personnel"),
    ACCUEIL("Salle d'accueil"),
    COMPTOIR("Comptoirs bancaires"),
    ENTREE("Entrée principale"),
    COFFRE("Salle des coffres"),
    PARKING("Parking");

    private final String description;

    ZoneType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}