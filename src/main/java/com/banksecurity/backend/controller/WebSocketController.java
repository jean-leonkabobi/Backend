package com.banksecurity.backend.controller;

import com.banksecurity.backend.dto.response.AlertResponse;
import com.banksecurity.backend.service.WebSocketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * Contrôleur WebSocket pour la communication en temps réel
 * Gère les messages STOMP et les endpoints REST pour les informations WebSocket
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@Tag(name = "WebSocket", description = "Communication temps réel")
public class WebSocketController {

    // Constantes pour éviter la duplication de littéraux
    private static final String STATUS_KEY = "status";
    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String STATUS_OK = "OK";
    private static final String STATUS_PONG = "PONG";

    private final WebSocketService webSocketService;

    /**
     * Endpoint STOMP pour envoyer une alerte
     * Client envoie vers /app/alert
     * Server répond vers /topic/alerts
     */
    @MessageMapping("/alert")
    @SendTo("/topic/alerts")
    @Operation(summary = "Envoyer une alerte en temps réel")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY', 'MANAGER')")
    public AlertResponse sendAlert(@Payload AlertResponse alert, Principal principal) {
        log.info("Alerte reçue via WebSocket de {}: {}",
                principal != null ? principal.getName() : "inconnu",
                alert.getId());
        return alert;
    }

    /**
     * Endpoint STOMP pour envoyer un message de test
     * Client envoie vers /app/test
     * Server répond vers /topic/test
     */
    @MessageMapping("/test")
    @SendTo("/topic/test")
    @Operation(summary = "Envoyer un message de test")
    public Map<String, String> testConnection(@Payload Map<String, String> message, Principal principal) {
        log.info("Message de test reçu de {}",
                principal != null ? principal.getName() : "inconnu");
        log.debug("Contenu du message de test: {}", message);

        return Map.of(
                STATUS_KEY, STATUS_OK,
                "message", "Connexion WebSocket établie",
                "user", principal != null ? principal.getName() : "inconnu",
                TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis())
        );
    }

    /**
     * Endpoint STOMP pour le ping/pong
     * Client envoie vers /app/ping
     * Server répond vers /topic/pong
     */
    @MessageMapping("/ping")
    @SendTo("/topic/pong")
    @Operation(summary = "Vérifier la connexion WebSocket")
    public Map<String, String> ping(Principal principal) {
        log.debug("Ping reçu de {}",
                principal != null ? principal.getName() : "inconnu");

        return Map.of(
                STATUS_KEY, STATUS_PONG,
                TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis())
        );
    }

    /**
     * Endpoint STOMP pour la connexion utilisateur
     * Client envoie vers /app/connect
     */
    @MessageMapping("/connect")
    @Operation(summary = "Enregistrer la connexion utilisateur")
    public void registerConnection(SimpMessageHeaderAccessor headerAccessor, Principal principal) {
        if (principal != null) {
            // Utiliser headerAccessor pour récupérer la session ID
            String sessionId = headerAccessor.getSessionId();
            log.debug("Session WebSocket: {}", sessionId);

            webSocketService.registerUserConnection(principal.getName());
            log.info("Utilisateur connecté via WebSocket: {}", principal.getName());
        }
    }

    /**
     * Endpoint STOMP pour la déconnexion utilisateur
     * Client envoie vers /app/disconnect
     */
    @MessageMapping("/disconnect")
    @Operation(summary = "Déconnecter l'utilisateur")
    public void unregisterConnection(Principal principal) {
        if (principal != null) {
            webSocketService.unregisterUserConnection(principal.getName());
            log.info("Utilisateur déconnecté via WebSocket: {}", principal.getName());
        }
    }

    /**
     * Endpoint STOMP pour diffuser le statut d'une caméra
     * Client envoie vers /app/camera-status
     * Server répond vers /topic/cameras
     */
    @MessageMapping("/camera-status")
    @SendTo("/topic/cameras")
    @Operation(summary = "Diffuser le statut d'une caméra")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Map<String, String> broadcastCameraStatus(@Payload Map<String, String> cameraStatus) {
        log.info("Statut caméra reçu: {}", cameraStatus);
        return cameraStatus;
    }
}

