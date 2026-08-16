package com.banksecurity.backend.service.impl;

import com.banksecurity.backend.dto.response.AlertResponse;
import com.banksecurity.backend.dto.response.DashboardStatsResponse;
import com.banksecurity.backend.service.WebSocketService;
import com.banksecurity.backend.util.AsyncUtils;
import com.banksecurity.backend.util.Constants;
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

    @Resource(name = "notificationExecutor")
    private Executor notificationExecutor;

    private final Map<String, Boolean> connectedUsers = new ConcurrentHashMap<>();

    @Override
    public void broadcastAlert(AlertResponse alert) {
        // ✅ Utilisation de Constants.WS_TOPIC_ALERTS
        CompletableFuture<Void> future = AsyncUtils.runAsync(
                () -> messagingTemplate.convertAndSend(Constants.WS_TOPIC_ALERTS, alert),
                notificationExecutor,
                "Diffusion alerte " + alert.getId()
        );
        future.exceptionally(e -> {
            log.error("Erreur lors de la diffusion de l'alerte: {}", e.getMessage());
            return null;
        });
        log.debug("Alerte diffusée via WebSocket (async): {}", alert.getId());
    }

    @Override
    public void sendAlertToUser(String username, AlertResponse alert) {
        // ✅ Utilisation de Constants.WS_QUEUE_USER
        CompletableFuture<Void> future = AsyncUtils.runAsync(
                () -> messagingTemplate.convertAndSendToUser(username, Constants.WS_QUEUE_USER + "/alerts", alert),
                notificationExecutor,
                "Envoi alerte à " + username
        );
        future.exceptionally(e -> {
            log.error("Erreur lors de l'envoi de l'alerte à {}: {}", username, e.getMessage());
            return null;
        });
        log.debug("Alerte envoyée à l'utilisateur {} (async): {}", username, alert.getId());
    }

    @Override
    public void broadcastStats(DashboardStatsResponse stats) {
        // ✅ Utilisation de Constants.WS_TOPIC_STATS
        CompletableFuture<Void> future = AsyncUtils.runAsync(
                () -> messagingTemplate.convertAndSend(Constants.WS_TOPIC_STATS, stats),
                notificationExecutor,
                "Diffusion statistiques"
        );
        future.exceptionally(e -> {
            log.error("Erreur lors de la diffusion des statistiques: {}", e.getMessage());
            return null;
        });
        log.debug("Statistiques diffusées via WebSocket (async)");
    }

    @Override
    public void broadcastCameraStatus(String cameraId, String status) {
        CameraStatusMessage message = new CameraStatusMessage(cameraId, status);
        // ✅ Utilisation de Constants.WS_TOPIC_CAMERAS
        CompletableFuture<Void> future = AsyncUtils.runAsync(
                () -> messagingTemplate.convertAndSend(Constants.WS_TOPIC_CAMERAS, message),
                notificationExecutor,
                "Diffusion statut caméra " + cameraId
        );
        future.exceptionally(e -> {
            log.error("Erreur lors de la diffusion du statut caméra: {}", e.getMessage());
            return null;
        });
        log.debug("Statut caméra diffusé (async): {} - {}", cameraId, status);
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