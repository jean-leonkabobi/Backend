package com.banksecurity.backend.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

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
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        log.info("Connexion WebSocket établie: {}", sessionId);
        log.debug("Nombre de sessions actives: {}", sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Message reçu de {}: {}", session.getId(), payload);

        // Répondre avec un accusé de réception
        session.sendMessage(new TextMessage("{\"status\":\"received\"}"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        log.info("Connexion WebSocket fermée: {} (statut: {})", sessionId, status);
        log.debug("Nombre de sessions actives: {}", sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        log.error("Erreur de transport WebSocket pour {}: {}", sessionId, exception.getMessage());

        // Fermer la session en cas d'erreur
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
        sessions.remove(sessionId);
    }

    /**
     * Envoie un message à une session spécifique
     */
    public void sendMessageToSession(String sessionId, String message) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (Exception e) {
                log.error("Erreur lors de l'envoi du message à {}: {}", sessionId, e.getMessage());
            }
        }
    }

    /**
     * Envoie un message à toutes les sessions
     */
    public void broadcastMessage(String message) {
        sessions.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (Exception e) {
                    log.error("Erreur lors de la diffusion du message à {}: {}",
                            session.getId(), e.getMessage());
                }
            }
        });
    }

    /**
     * Récupère le nombre de sessions actives
     */
    public int getActiveSessionsCount() {
        return sessions.size();
    }
}