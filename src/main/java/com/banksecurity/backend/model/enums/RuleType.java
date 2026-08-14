package com.banksecurity.backend.model.enums;

public enum RuleType {
    INTRUSION("Détection d'intrusion"),
    PRESENCE_PROLONGEE("Présence prolongée"),
    OBJET_SUSPECT("Objet suspect"),
    SKIMMER("Détection de skimmer"),
    ARME("Détection d'arme"),
    CHUTE("Détection de chute"),
    RODEUR("Détection de rôdeur"),
    MASQUE("Détection de masque");

    private final String description;

    RuleType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}