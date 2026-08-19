package com.banksecurity.backend.service.impl;

import com.banksecurity.backend.dto.response.AlertResponse;
import com.banksecurity.backend.dto.response.DashboardStatsResponse;
import com.banksecurity.backend.service.WebSocketService;
import com.banksecurity.backend.util.AsyncUtils;
import com.banksecurity.backend.util.Constants;
import com.banksecurity.backend.websocket.WebSocketHandler;
import com.banksecurity.backend.websocket.WebSocketMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
        // ✅ Utilisation de WebSocketMessage.alertCreated()
        WebSocketMessage wsMessage = WebSocketMessage.alertCreated(convertToJson(alert));
        String messageJson = convertToJson(wsMessage);

        // Diffusion via WebSocketHandler
        webSocketHandler.broadcastMessage(messageJson);
        log.debug("Alerte diffusée via WebSocketHandler: {}", alert.getId());

        // Diffusion via SimpMessagingTemplate pour les clients STOMP
        CompletableFuture<Void> future = AsyncUtils.runAsync(
                () -> messagingTemplate.convertAndSend(Constants.WS_TOPIC_ALERTS, wsMessage),
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
        // ✅ Utilisation de WebSocketMessage avec destinataire
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.ALERT_CREATED)
                .content(convertToJson(alert))
                .recipient(username)
                .timestamp(LocalDateTime.now())
                .build();
        String messageJson = convertToJson(wsMessage);

        // Envoi direct via WebSocketHandler
        webSocketHandler.sendMessageToSession(username, messageJson);
        log.debug("Alerte envoyée via WebSocketHandler à {}: {}", username, alert.getId());

        // Envoi via SimpMessagingTemplate pour les clients STOMP
        CompletableFuture<Void> future = AsyncUtils.runAsync(
                () -> messagingTemplate.convertAndSendToUser(username, Constants.WS_QUEUE_USER + "/alerts", wsMessage),
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
        // ✅ Utilisation de WebSocketMessage avec STATS_UPDATED
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.STATS_UPDATED)
                .content(convertToJson(stats))
                .timestamp(LocalDateTime.now())
                .build();
        String messageJson = convertToJson(wsMessage);

        webSocketHandler.broadcastMessage(messageJson);
        log.debug("Statistiques diffusées via WebSocketHandler");

        CompletableFuture<Void> future = AsyncUtils.runAsync(
                () -> messagingTemplate.convertAndSend(Constants.WS_TOPIC_STATS, wsMessage),
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
        // ✅ Utilisation de WebSocketMessage avec CAMERA_STATUS_CHANGED
        CameraStatusMessage cameraMessage = new CameraStatusMessage(cameraId, status);
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.CAMERA_STATUS_CHANGED)
                .content(convertToJson(cameraMessage))
                .timestamp(LocalDateTime.now())
                .build();
        String messageJson = convertToJson(wsMessage);

        webSocketHandler.broadcastMessage(messageJson);
        log.debug("Statut caméra diffusé via WebSocketHandler: {} - {}", cameraId, status);

        CompletableFuture<Void> future = AsyncUtils.runAsync(
                () -> messagingTemplate.convertAndSend(Constants.WS_TOPIC_CAMERAS, wsMessage),
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

        // ✅ Utilisation de WebSocketMessage avec USER_CONNECTED
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.USER_CONNECTED)
                .content(username)
                .timestamp(LocalDateTime.now())
                .build();
        webSocketHandler.broadcastMessage(convertToJson(wsMessage));

        log.debug("Utilisateur connecté: {}", username);
    }

    public void unregisterUserConnection(String username) {
        connectedUsers.remove(username);

        // ✅ Utilisation de WebSocketMessage avec USER_DISCONNECTED
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.USER_DISCONNECTED)
                .content(username)
                .timestamp(LocalDateTime.now())
                .build();
        webSocketHandler.broadcastMessage(convertToJson(wsMessage));

        log.debug("Utilisateur déconnecté: {}", username);
    }

    // ==================== NOUVELLES MÉTHODES UTILISANT WebSocketMessage ====================

    /**
     * ✅ Utilisation de WebSocketMessage.ping()
     */
    public void sendPing() {
        WebSocketMessage pingMessage = WebSocketMessage.ping();
        webSocketHandler.broadcastMessage(convertToJson(pingMessage));
        log.debug("Ping envoyé à toutes les sessions");
    }

    /**
     * ✅ Utilisation de WebSocketMessage.pong()
     */
    public void sendPong() {
        WebSocketMessage pongMessage = WebSocketMessage.pong();
        webSocketHandler.broadcastMessage(convertToJson(pongMessage));
        log.debug("Pong envoyé à toutes les sessions");
    }

    /**
     * ✅ Utilisation de WebSocketMessage.systemNotification()
     */
    public void sendSystemNotification(String content, String recipient) {
        WebSocketMessage notification = WebSocketMessage.systemNotification(content, recipient);
        webSocketHandler.broadcastMessage(convertToJson(notification));
        log.debug("Notification système envoyée à {}: {}", recipient, content);
    }

    /**
     * ✅ Utilisation de WebSocketMessage avec ALERT_UPDATED
     */
    public void broadcastAlertUpdated(String alertJson) {
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.ALERT_UPDATED)
                .content(alertJson)
                .timestamp(LocalDateTime.now())
                .build();
        webSocketHandler.broadcastMessage(convertToJson(wsMessage));
    }

    /**
     * ✅ Utilisation de WebSocketMessage avec ALERT_RESOLVED
     */
    public void broadcastAlertResolved(String alertJson) {
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.ALERT_RESOLVED)
                .content(alertJson)
                .timestamp(LocalDateTime.now())
                .build();
        webSocketHandler.broadcastMessage(convertToJson(wsMessage));
    }

    /**
     * ✅ Utilisation de WebSocketMessage avec ALERT_ESCALATED
     */
    public void broadcastAlertEscalated(String alertJson) {
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.ALERT_ESCALATED)
                .content(alertJson)
                .timestamp(LocalDateTime.now())
                .build();
        webSocketHandler.broadcastMessage(convertToJson(wsMessage));
    }

    /**
     * ✅ Utilisation de WebSocketMessage avec CAMERA_ADDED
     */
    public void broadcastCameraAdded(String cameraJson) {
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.CAMERA_ADDED)
                .content(cameraJson)
                .timestamp(LocalDateTime.now())
                .build();
        webSocketHandler.broadcastMessage(convertToJson(wsMessage));
    }

    /**
     * ✅ Utilisation de WebSocketMessage avec CAMERA_REMOVED
     */
    public void broadcastCameraRemoved(String cameraJson) {
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.CAMERA_REMOVED)
                .content(cameraJson)
                .timestamp(LocalDateTime.now())
                .build();
        webSocketHandler.broadcastMessage(convertToJson(wsMessage));
    }

    /**
     * ✅ Utilisation de WebSocketMessage avec ZONE_UPDATED
     */
    public void broadcastZoneUpdate(String zoneJson) {
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.ZONE_UPDATED)
                .content(zoneJson)
                .timestamp(LocalDateTime.now())
                .build();
        webSocketHandler.broadcastMessage(convertToJson(wsMessage));
    }

    /**
     * ✅ Utilisation de WebSocketMessage avec RULE_TRIGGERED
     */
    public void broadcastRuleTriggered(String ruleJson) {
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.RULE_TRIGGERED)
                .content(ruleJson)
                .timestamp(LocalDateTime.now())
                .build();
        webSocketHandler.broadcastMessage(convertToJson(wsMessage));
    }

    /**
     * ✅ Utilisation de WebSocketMessage avec ERROR
     */
    public void broadcastError(String errorMessage) {
        WebSocketMessage wsMessage = WebSocketMessage.builder()
                .type(WebSocketMessage.MessageType.ERROR)
                .content(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
        webSocketHandler.broadcastMessage(convertToJson(wsMessage));
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