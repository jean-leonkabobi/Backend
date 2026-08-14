package com.banksecurity.backend.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Message WebSocket standardisé
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

    /**
     * Type de message
     */
    private MessageType type;

    /**
     * Contenu du message (JSON)
     */
    private String content;

    /**
     * Horodatage du message
     */
    private LocalDateTime timestamp;

    /**
     * Expéditeur (optionnel)
     */
    private String sender;

    /**
     * Destinataire (optionnel, null pour diffusion)
     */
    private String recipient;

    /**
     * Types de messages WebSocket
     */
    public enum MessageType {
        ALERT_CREATED,
        ALERT_UPDATED,
        ALERT_RESOLVED,
        ALERT_ESCALATED,
        CAMERA_STATUS_CHANGED,
        CAMERA_ADDED,
        CAMERA_REMOVED,
        ZONE_UPDATED,
        RULE_TRIGGERED,
        STATS_UPDATED,
        SYSTEM_NOTIFICATION,
        USER_CONNECTED,
        USER_DISCONNECTED,
        ERROR,
        PING,
        PONG
    }

    /**
     * Crée un message d'alerte
     */
    public static WebSocketMessage alertCreated(String alertJson) {
        return WebSocketMessage.builder()
                .type(MessageType.ALERT_CREATED)
                .content(alertJson)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crée un message de notification système
     */
    public static WebSocketMessage systemNotification(String content, String recipient) {
        return WebSocketMessage.builder()
                .type(MessageType.SYSTEM_NOTIFICATION)
                .content(content)
                .recipient(recipient)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crée un message de ping
     */
    public static WebSocketMessage ping() {
        return WebSocketMessage.builder()
                .type(MessageType.PING)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crée un message de pong
     */
    public static WebSocketMessage pong() {
        return WebSocketMessage.builder()
                .type(MessageType.PONG)
                .timestamp(LocalDateTime.now())
                .build();
    }
}