package com.banksecurity.backend.service.impl;

import com.banksecurity.backend.dto.response.AlertResponse;
import com.banksecurity.backend.dto.response.DashboardStatsResponse;
import com.banksecurity.backend.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketServiceImpl implements WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    // Map pour suivre les utilisateurs connectés
    private final Map<String, Boolean> connectedUsers = new ConcurrentHashMap<>();

    @Override
    public void broadcastAlert(AlertResponse alert) {
        messagingTemplate.convertAndSend("/topic/alerts", alert);
        log.debug("Alerte diffusée via WebSocket: {}", alert.getId());
    }

    @Override
    public void sendAlertToUser(String username, AlertResponse alert) {
        messagingTemplate.convertAndSendToUser(username, "/queue/alerts", alert);
        log.debug("Alerte envoyée à l'utilisateur {}: {}", username, alert.getId());
    }

    @Override
    public void broadcastStats(DashboardStatsResponse stats) {
        messagingTemplate.convertAndSend("/topic/stats", stats);
        log.debug("Statistiques diffusées via WebSocket");
    }

    @Override
    public void broadcastCameraStatus(String cameraId, String status) {
        // Correction : Utiliser un objet DTO au lieu de Map.of()
        CameraStatusMessage message = new CameraStatusMessage(cameraId, status);
        messagingTemplate.convertAndSend("/topic/cameras", message);
        log.debug("Statut caméra diffusé: {} - {}", cameraId, status);
    }

    @Override
    public void sendToTopic(String topic, Object message) {
        messagingTemplate.convertAndSend(topic, message);
        log.debug("Message envoyé au topic {}: {}", topic, message);
    }

    @Override
    public boolean isUserConnected(String username) {
        return connectedUsers.getOrDefault(username, false);
    }

    @Override
    public int getConnectedClientsCount() {
        return connectedUsers.size();
    }

    /**
     * Enregistre une connexion utilisateur
     */
    public void registerUserConnection(String username) {
        connectedUsers.put(username, true);
        log.debug("Utilisateur connecté: {}", username);
    }

    /**
     * Déconnecte un utilisateur
     */
    public void unregisterUserConnection(String username) {
        connectedUsers.remove(username);
        log.debug("Utilisateur déconnecté: {}", username);
    }

    /**
     * Classe interne pour les messages de statut caméra
     */
    private static class CameraStatusMessage {
        private final String cameraId;
        private final String status;

        public CameraStatusMessage(String cameraId, String status) {
            this.cameraId = cameraId;
            this.status = status;
        }

        public String getCameraId() {
            return cameraId;
        }

        public String getStatus() {
            return status;
        }
    }
}