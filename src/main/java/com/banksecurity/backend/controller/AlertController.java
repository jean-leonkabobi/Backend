package com.banksecurity.backend.controller;

import com.banksecurity.backend.dto.request.AlertRequest;
import com.banksecurity.backend.dto.request.AlertStatusUpdateRequest;
import com.banksecurity.backend.dto.response.AlertResponse;
import com.banksecurity.backend.dto.response.ApiResponse;
import com.banksecurity.backend.model.enums.AlertSeverity;
import com.banksecurity.backend.model.enums.AlertStatus;
import com.banksecurity.backend.service.AlertService;
import com.banksecurity.backend.util.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
@Tag(name = "Alertes", description = "Gestion des alertes de sécurité")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @Operation(summary = "Récupérer toutes les alertes")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getAllAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE_SIZE + "") int size) {

        List<AlertResponse> alerts = alertService.getAllAlerts();
        // ✅ Utilisation de la méthode utilitaire
        List<AlertResponse> paginatedAlerts = paginate(alerts, page, size);
        return ResponseEntity.ok(ApiResponse.success("Alertes récupérées", paginatedAlerts));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer une alerte par ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY', 'MANAGER')")
    public ResponseEntity<ApiResponse<AlertResponse>> getAlertById(@PathVariable UUID id) {
        AlertResponse alert = alertService.getAlertById(id);
        return ResponseEntity.ok(ApiResponse.success("Alerte récupérée", alert));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Récupérer les alertes par statut")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getAlertsByStatus(
            @PathVariable AlertStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE_SIZE + "") int size) {

        List<AlertResponse> alerts = alertService.getAlertsByStatus(status);
        // ✅ Utilisation de la méthode utilitaire
        List<AlertResponse> paginatedAlerts = paginate(alerts, page, size);
        return ResponseEntity.ok(ApiResponse.success("Alertes récupérées", paginatedAlerts));
    }

    @GetMapping("/severity/{severity}")
    @Operation(summary = "Récupérer les alertes par sévérité")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getAlertsBySeverity(
            @PathVariable AlertSeverity severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE_SIZE + "") int size) {

        List<AlertResponse> alerts = alertService.getAlertsBySeverity(severity);
        // ✅ Utilisation de la méthode utilitaire
        List<AlertResponse> paginatedAlerts = paginate(alerts, page, size);
        return ResponseEntity.ok(ApiResponse.success("Alertes récupérées", paginatedAlerts));
    }

    @GetMapping("/camera/{cameraId}")
    @Operation(summary = "Récupérer les alertes d'une caméra")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getAlertsByCamera(@PathVariable UUID cameraId) {
        List<AlertResponse> alerts = alertService.getAlertsByCamera(cameraId);
        return ResponseEntity.ok(ApiResponse.success("Alertes récupérées", alerts));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Récupérer les alertes par période")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getAlertsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<AlertResponse> alerts = alertService.getAlertsByDateRange(start, end);
        return ResponseEntity.ok(ApiResponse.success("Alertes récupérées", alerts));
    }

    @GetMapping("/critical/unresolved")
    @Operation(summary = "Récupérer les alertes critiques non résolues")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<AlertResponse>>> getUnresolvedCriticalAlerts() {
        List<AlertResponse> alerts = alertService.getUnresolvedCriticalAlerts();
        return ResponseEntity.ok(ApiResponse.success("Alertes critiques récupérées", alerts));
    }

    @PostMapping
    @Operation(summary = "Créer une nouvelle alerte")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY', 'MANAGER')")
    public ResponseEntity<ApiResponse<AlertResponse>> createAlert(@Valid @RequestBody AlertRequest request) {
        AlertResponse alert = alertService.createAlert(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Alerte créée", alert));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Mettre à jour le statut d'une alerte")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY', 'MANAGER')")
    public ResponseEntity<ApiResponse<AlertResponse>> updateAlertStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AlertStatusUpdateRequest request) {
        AlertResponse alert = alertService.updateAlertStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Statut de l'alerte mis à jour", alert));
    }

    @PostMapping("/{id}/escalate")
    @Operation(summary = "Escalader une alerte")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY')")
    public ResponseEntity<ApiResponse<AlertResponse>> escalateAlert(@PathVariable UUID id) {
        AlertResponse alert = alertService.escalateAlert(id);
        return ResponseEntity.ok(ApiResponse.success("Alerte escaladée", alert));
    }

    @PostMapping("/{id}/resolve")
    @Operation(summary = "Résoudre une alerte")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY', 'MANAGER')")
    public ResponseEntity<ApiResponse<AlertResponse>> resolveAlert(
            @PathVariable UUID id,
            @RequestParam AlertStatus status,
            @RequestParam(required = false) String notes) {
        AlertResponse alert = alertService.resolveAlert(id, status, notes);
        return ResponseEntity.ok(ApiResponse.success("Alerte résolue", alert));
    }

    /**
     * Méthode utilitaire pour paginer une liste
     * ✅ Évite la duplication de code
     */
    private List<AlertResponse> paginate(List<AlertResponse> alerts, int page, int size) {
        int safeSize = Math.min(size, Constants.MAX_PAGE_SIZE);
        int start = Math.min(page * safeSize, alerts.size());
        int end = Math.min(start + safeSize, alerts.size());

        if (start >= alerts.size()) {
            return List.of();
        }
        return alerts.subList(start, end);
    }
}