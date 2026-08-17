package com.banksecurity.backend.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire WebSocket pour les connexions temps réel
 */
@Slf4j
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    // Sessions WebSocket actives
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        log.info("[WS-CONNECT] Connexion établie: {}", sessionId);
        log.debug("[WS-CONNECT] Nombre de sessions: {}", sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String payload = message.getPayload();
        log.debug("[WS-MESSAGE] Message reçu de {}: {}", session.getId(), payload);

        // ✅ Pas de try-with-resources - WebSocketSession n'est pas AutoCloseable
        session.sendMessage(new TextMessage("{\"status\":\"received\"}"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        log.info("[WS-CLOSE] Connexion fermée: {} (statut: {})", sessionId, status);
        log.debug("[WS-CLOSE] Nombre de sessions: {}", sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws IOException {
        String sessionId = session.getId();
        log.error("[WS-ERROR] Erreur de transport pour {}: {}", sessionId, exception.getMessage());

        // ✅ Pas de try-with-resources
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
        sessions.remove(sessionId);
    }

    /**
     * Envoie un message à une session spécifique
     * Utilisé par WebSocketServiceImpl via sendAlertToUser
     */
    public void sendMessageToSession(String sessionId, String message) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                // ✅ Pas de try-with-resources
                session.sendMessage(new TextMessage(message));
                log.debug("[WS-SEND-ONE] Message envoyé à: {}", sessionId);
            } catch (IOException e) {
                log.error("[WS-SEND-ERROR] Erreur d'envoi à {}: {}", sessionId, e.getMessage());
            }
        } else {
            log.warn("[WS-SEND-WARN] Session non trouvée: {}", sessionId);
        }
    }

    /**
     * Envoie un message à toutes les sessions
     * Utilisé par WebSocketServiceImpl via broadcastAlert
     */
    public void broadcastMessage(String message) {
        sessions.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    // ✅ Pas de try-with-resources
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    log.error("[WS-BROADCAST-ERROR] Erreur de diffusion à {}: {}",
                            session.getId(), e.getMessage());
                }
            }
        });
        log.debug("[WS-BROADCAST] Message diffusé à {} sessions", sessions.size());
    }

    /**
     * Récupère le nombre de sessions actives
     */
    public int getActiveSessionsCount() {
        return sessions.size();
    }
}