package com.banksecurity.backend.service;

import com.banksecurity.backend.dto.request.AlertRequest;
import com.banksecurity.backend.dto.request.AlertStatusUpdateRequest;
import com.banksecurity.backend.dto.response.AlertResponse;
import com.banksecurity.backend.model.enums.AlertSeverity;
import com.banksecurity.backend.model.enums.AlertStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AlertService {

    /**
     * Crée une nouvelle alerte
     */
    AlertResponse createAlert(AlertRequest request);

    /**
     * Met à jour le statut d'une alerte
     */
    AlertResponse updateAlertStatus(UUID id, AlertStatusUpdateRequest request);

    /**
     * Récupère une alerte par son ID
     */
    AlertResponse getAlertById(UUID id);

    /**
     * Récupère toutes les alertes
     */
    List<AlertResponse> getAllAlerts();

    /**
     * Récupère les alertes par statut
     */
    List<AlertResponse> getAlertsByStatus(AlertStatus status);

    /**
     * Récupère les alertes par sévérité
     */
    List<AlertResponse> getAlertsBySeverity(AlertSeverity severity);

    /**
     * Récupère les alertes par caméra
     */
    List<AlertResponse> getAlertsByCamera(UUID cameraId);

    /**
     * Récupère les alertes par zone
     */
    List<AlertResponse> getAlertsByZone(UUID zoneId);

    /**
     * Récupère les alertes par période
     */
    List<AlertResponse> getAlertsByDateRange(LocalDateTime start, LocalDateTime end);

    /**
     * Récupère les alertes critiques non résolues
     */
    List<AlertResponse> getUnresolvedCriticalAlerts();

    /**
     * Escalade une alerte
     */
    AlertResponse escalateAlert(UUID id);

    /**
     * Résout une alerte (statut RESOLVED ou FALSE_ALARM)
     */
    AlertResponse resolveAlert(UUID id, AlertStatus resolutionStatus, String notes);

    /**
     * Récupère le nombre total d'alertes
     */
    long countAlerts();

    /**
     * Récupère le nombre d'alertes par statut
     */
    long countAlertsByStatus(AlertStatus status);

    /**
     * Récupère le nombre d'alertes depuis une date
     */
    long countAlertsSince(LocalDateTime since);

    /**
     * Sauvegarde l'image associée à une alerte
     */
    String saveAlertImage(UUID alertId, byte[] imageData);
}