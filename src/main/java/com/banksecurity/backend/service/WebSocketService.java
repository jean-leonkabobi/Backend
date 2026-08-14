package com.banksecurity.backend.service;

import com.banksecurity.backend.dto.response.AlertResponse;
import com.banksecurity.backend.dto.response.DashboardStatsResponse;

public interface WebSocketService {

    /**
     * Envoie une alerte en temps réel à tous les clients
     */
    void broadcastAlert(AlertResponse alert);

    /**
     * Envoie une alerte à un utilisateur spécifique
     */
    void sendAlertToUser(String username, AlertResponse alert);

    /**
     * Envoie les statistiques mises à jour
     */
    void broadcastStats(DashboardStatsResponse stats);

    /**
     * Envoie une notification de caméra
     */
    void broadcastCameraStatus(String cameraId, String status);

    /**
     * Envoie un message à un topic spécifique
     */
    void sendToTopic(String topic, Object message);

    /**
     * Vérifie si un utilisateur est connecté
     */
    boolean isUserConnected(String username);

    /**
     * Récupère le nombre de clients connectés
     */
    int getConnectedClientsCount();
}