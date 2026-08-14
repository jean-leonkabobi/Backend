package com.banksecurity.backend.model.enums;

public enum AlertSeverity {
    CRITICAL("Critique", 4),
    HIGH("Élevée", 3),
    MEDIUM("Moyenne", 2),
    INFO("Informative", 1);

    private final String label;
    private final int priority;

    AlertSeverity(String label, int priority) {
        this.label = label;
        this.priority = priority;
    }

    public String getLabel() {
        return label;
    }

    public int getPriority() {
        return priority;
    }
}