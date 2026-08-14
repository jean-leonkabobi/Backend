package com.banksecurity.backend.controller;

import com.banksecurity.backend.dto.response.ApiResponse;
import com.banksecurity.backend.service.WebSocketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Contrôleur REST pour les informations WebSocket
 */
@Slf4j
@RestController
@RequestMapping("/ws")
@RequiredArgsConstructor
@Tag(name = "WebSocket", description = "Informations WebSocket")
class WebSocketInfoController {

    private static final String STATUS_KEY = "status";
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final WebSocketService webSocketService;

    @GetMapping("/info")
    @Operation(summary = "Récupérer les informations WebSocket")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWebSocketInfo() {
        Map<String, Object> info = Map.of(
                "endpoint", "/ws",
                "connectedClients", webSocketService.getConnectedClientsCount(),
                "topics", new String[]{
                        "/topic/alerts",
                        "/topic/cameras",
                        "/topic/stats",
                        "/topic/test",
                        "/topic/pong"
                },
                STATUS_KEY, STATUS_ACTIVE
        );

        return ResponseEntity.ok(ApiResponse.success("Informations WebSocket récupérées", info));
    }

    @GetMapping("/connected-clients")
    @Operation(summary = "Récupérer le nombre de clients connectés")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Integer>> getConnectedClientsCount() {
        int count = webSocketService.getConnectedClientsCount();
        return ResponseEntity.ok(ApiResponse.success("Nombre de clients connectés", count));
    }

    @GetMapping("/is-connected")
    @Operation(summary = "Vérifier si un utilisateur est connecté")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Boolean>> isUserConnected(@RequestParam String username) {
        boolean isConnected = webSocketService.isUserConnected(username);
        return ResponseEntity.ok(ApiResponse.success("Statut de connexion", isConnected));
    }
}
