package com.banksecurity.backend.service.impl;

import com.banksecurity.backend.dto.response.AlertResponse;
import com.banksecurity.backend.dto.response.DashboardStatsResponse;
import com.banksecurity.backend.service.WebSocketService;
import com.banksecurity.backend.util.AsyncUtils;
import com.banksecurity.backend.util.Constants;
import com.banksecurity.backend.websocket.WebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketServiceImpl implements WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource(name = "notificationExecutor")
    private Executor notificationExecutor;

    private final Map<String, Boolean> connectedUsers = new ConcurrentHashMap<>();

    @Override
    public void broadcastAlert(AlertResponse alert) {
        // ✅ Utilisation de WebSocketHandler.broadcastMessage pour la diffusion directe
        String messageJson = convertToJson(alert);
        webSocketHandler.broadcastMessage(messageJson);
        log.debug("Alerte diffusée via WebSocketHandler: {}", alert.getId());

        // ✅ Également via SimpMessagingTemplate pour les clients STOMP
        CompletableFuture<Void> future = AsyncUtils.runAsync(
                () -> messagingTemplate.convertAndSend(Constants.WS_TOPIC_ALERTS, alert),
                notificationExecutor,
                "Diffusion alerte " + alert.getId()
        );
        future.exceptionally(e -> {
            log.error("Erreur lors de la diffusion de l'alerte: {}", e.getMessage());
            return null;
        });
    }

    @Override
    public void sendAlertToUser(String username, AlertResponse alert) {
        // ✅ Utilisation de WebSocketHandler.sendMessageToSession pour l'envoi direct
        String messageJson = convertToJson(alert);
        webSocketHandler.sendMessageToSession(username, messageJson);
        log.debug("Alerte envoyée via WebSocketHandler à {}: {}", username, alert.getId());

        // ✅ Également via SimpMessagingTemplate pour les clients STOMP
        CompletableFuture<Void> future = AsyncUtils.runAsync(
                () -> messagingTemplate.convertAndSendToUser(username, Constants.WS_QUEUE_USER + "/alerts", alert),
                notificationExecutor,
                "Envoi alerte à " + username
        );
        future.exceptionally(e -> {
            log.error("Erreur lors de l'envoi de l'alerte à {}: {}", username, e.getMessage());
            return null;
        });
    }

    @Override
    public void broadcastStats(DashboardStatsResponse stats) {
        // ✅ Utilisation de WebSocketHandler.broadcastMessage pour les statistiques
        String messageJson = convertToJson(stats);
        webSocketHandler.broadcastMessage(messageJson);
        log.debug("Statistiques diffusées via WebSocketHandler");

        // ✅ Également via SimpMessagingTemplate pour les clients STOMP
        CompletableFuture<Void> future = AsyncUtils.runAsync(
                () -> messagingTemplate.convertAndSend(Constants.WS_TOPIC_STATS, stats),
                notificationExecutor,
                "Diffusion statistiques"
        );
        future.exceptionally(e -> {
            log.error("Erreur lors de la diffusion des statistiques: {}", e.getMessage());
            return null;
        });
    }

    @Override
    public void broadcastCameraStatus(String cameraId, String status) {
        CameraStatusMessage message = new CameraStatusMessage(cameraId, status);

        // ✅ Utilisation de WebSocketHandler.broadcastMessage pour le statut caméra
        String messageJson = convertToJson(message);
        webSocketHandler.broadcastMessage(messageJson);
        log.debug("Statut caméra diffusé via WebSocketHandler: {} - {}", cameraId, status);

        // ✅ Également via SimpMessagingTemplate pour les clients STOMP
        CompletableFuture<Void> future = AsyncUtils.runAsync(
                () -> messagingTemplate.convertAndSend(Constants.WS_TOPIC_CAMERAS, message),
                notificationExecutor,
                "Diffusion statut caméra " + cameraId
        );
        future.exceptionally(e -> {
            log.error("Erreur lors de la diffusion du statut caméra: {}", e.getMessage());
            return null;
        });
    }

    @Override
    public void sendToTopic(String topic, Object message) {
        CompletableFuture<Void> future = AsyncUtils.runAsync(
                () -> messagingTemplate.convertAndSend(topic, message),
                notificationExecutor,
                "Envoi au topic " + topic
        );
        future.exceptionally(e -> {
            log.error("Erreur lors de l'envoi au topic {}: {}", topic, e.getMessage());
            return null;
        });
        log.debug("Message envoyé au topic {} (async)", topic);
    }

    @Override
    public boolean isUserConnected(String username) {
        return connectedUsers.getOrDefault(username, false);
    }

    @Override
    public int getConnectedClientsCount() {
        return connectedUsers.size();
    }

    public void registerUserConnection(String username) {
        connectedUsers.put(username, true);
        log.debug("Utilisateur connecté: {}", username);
    }

    public void unregisterUserConnection(String username) {
        connectedUsers.remove(username);
        log.debug("Utilisateur déconnecté: {}", username);
    }

    /**
     * Convertit un objet en JSON pour la diffusion via WebSocketHandler
     */
    private String convertToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            log.error("Erreur de conversion JSON: {}", e.getMessage());
            return "{}";
        }
    }

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