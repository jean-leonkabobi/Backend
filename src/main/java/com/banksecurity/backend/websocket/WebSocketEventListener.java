package com.banksecurity.backend.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Écouteur d'événements WebSocket
 * Gère les connexions et déconnexions
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final WebSocketHandler webSocketHandler;

    /**
     * Gère l'événement de connexion
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = headers.getSessionId();

        if (sessionId != null) {
            log.info("Nouvelle connexion WebSocket: {}", sessionId);
            // ✅ Utilisation de webSocketHandler pour le suivi des sessions
            log.debug("Sessions actives après connexion: {}", webSocketHandler.getActiveSessionsCount());
        }
    }

    /**
     * Gère l'événement de déconnexion
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headers.getSessionId();

        if (sessionId != null) {
            log.info("Déconnexion WebSocket: {}", sessionId);
            // ✅ Utilisation de webSocketHandler pour le suivi des sessions
            log.debug("Sessions actives après déconnexion: {}", webSocketHandler.getActiveSessionsCount());
        }
    }
}