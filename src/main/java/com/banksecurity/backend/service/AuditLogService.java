package com.banksecurity.backend.service;

import com.banksecurity.backend.model.AuditLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditLogService {

    /**
     * Enregistre une action d'audit
     */
    void logAction(UUID userId, String action, String details);

    /**
     * Enregistre une action d'audit avec informations supplémentaires
     */
    void logAction(UUID userId, String action, String details, String ipAddress, String userAgent);

    /**
     * Enregistre une action d'audit sur une entité
     */
    void logEntityAction(UUID userId, String action, String entityType, UUID entityId, String details);

    /**
     * Récupère les logs d'audit par utilisateur
     */
    List<AuditLog> getLogsByUser(UUID userId);

    /**
     * Récupère les logs d'audit par action
     */
    List<AuditLog> getLogsByAction(String action);

    /**
     * Récupère les logs d'audit par période
     */
    List<AuditLog> getLogsByDateRange(LocalDateTime start, LocalDateTime end);

    /**
     * Récupère les logs d'audit récents
     */
    List<AuditLog> getRecentLogs(int limit);

    /**
     * Récupère les actions échouées
     */
    List<AuditLog> getFailedActions();

    /**
     * Nettoie les logs plus anciens qu'une date
     */
    void cleanupOldLogs(LocalDateTime before);

    /**
     * Récupère le nombre total de logs
     */
    long countLogs();
}