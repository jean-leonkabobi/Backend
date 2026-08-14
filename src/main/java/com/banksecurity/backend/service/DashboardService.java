package com.banksecurity.backend.service;

import com.banksecurity.backend.dto.response.DashboardStatsResponse;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public interface DashboardService {

    /**
     * Récupère les statistiques globales du dashboard
     */
    DashboardStatsResponse getGlobalStats();

    /**
     * Récupère les statistiques pour une période donnée
     */
    DashboardStatsResponse getStatsForPeriod(LocalDateTime start, LocalDateTime end);

    /**
     * Récupère les statistiques des alertes par heure (24h)
     */
    Map<Integer, Long> getAlertsByHour();

    /**
     * Récupère les statistiques des alertes par type
     */
    Map<String, Long> getAlertsByType();

    /**
     * Récupère les statistiques des alertes par caméra
     */
    Map<String, Long> getAlertsByCamera();

    /**
     * Récupère les caméras avec le plus d'alertes
     */
    Map<UUID, Long> getTopCamerasByAlerts(int limit);

    /**
     * Calcule la tendance des alertes (comparaison avec la période précédente)
     */
    double calculateAlertTrend();

    /**
     * Récupère les statistiques système
     */
    DashboardStatsResponse.SystemInfo getSystemInfo();

    /**
     * Récupère les alertes en attente de traitement
     */
    long getPendingAlertsCount();

    /**
     * Récupère les alertes escaladées
     */
    long getEscalatedAlertsCount();
}