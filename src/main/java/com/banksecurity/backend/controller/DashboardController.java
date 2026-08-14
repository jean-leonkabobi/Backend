package com.banksecurity.backend.controller;

import com.banksecurity.backend.dto.response.ApiResponse;
import com.banksecurity.backend.dto.response.DashboardStatsResponse;
import com.banksecurity.backend.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Tableau de bord", description = "Statistiques et indicateurs")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "Récupérer les statistiques globales")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY', 'MANAGER')")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getGlobalStats() {
        DashboardStatsResponse stats = dashboardService.getGlobalStats();
        return ResponseEntity.ok(ApiResponse.success("Statistiques récupérées", stats));
    }

    @GetMapping("/stats/period")
    @Operation(summary = "Récupérer les statistiques pour une période")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY', 'MANAGER')")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStatsForPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        DashboardStatsResponse stats = dashboardService.getStatsForPeriod(start, end);
        return ResponseEntity.ok(ApiResponse.success("Statistiques récupérées", stats));
    }

    @GetMapping("/alerts-by-hour")
    @Operation(summary = "Récupérer les alertes par heure")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<Integer, Long>>> getAlertsByHour() {
        Map<Integer, Long> alertsByHour = dashboardService.getAlertsByHour();
        return ResponseEntity.ok(ApiResponse.success("Alertes par heure récupérées", alertsByHour));
    }

    @GetMapping("/alerts-by-type")
    @Operation(summary = "Récupérer les alertes par type")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getAlertsByType() {
        Map<String, Long> alertsByType = dashboardService.getAlertsByType();
        return ResponseEntity.ok(ApiResponse.success("Alertes par type récupérées", alertsByType));
    }

    @GetMapping("/alerts-by-camera")
    @Operation(summary = "Récupérer les alertes par caméra")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getAlertsByCamera() {
        Map<String, Long> alertsByCamera = dashboardService.getAlertsByCamera();
        return ResponseEntity.ok(ApiResponse.success("Alertes par caméra récupérées", alertsByCamera));
    }

    @GetMapping("/system-info")
    @Operation(summary = "Récupérer les informations système")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<DashboardStatsResponse.SystemInfo>> getSystemInfo() {
        DashboardStatsResponse.SystemInfo systemInfo = dashboardService.getSystemInfo();
        return ResponseEntity.ok(ApiResponse.success("Informations système récupérées", systemInfo));
    }

    @GetMapping("/pending-alerts-count")
    @Operation(summary = "Récupérer le nombre d'alertes en attente")
    @PreAuthorize("hasAnyRole('ADMIN', 'SECURITY', 'MANAGER')")
    public ResponseEntity<ApiResponse<Long>> getPendingAlertsCount() {
        long count = dashboardService.getPendingAlertsCount();
        return ResponseEntity.ok(ApiResponse.success("Nombre d'alertes en attente", count));
    }
}