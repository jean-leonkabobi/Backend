package com.banksecurity.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    // Statistiques générales
    private long totalCameras;
    private long activeCameras;
    private long camerasInError;
    private long totalZones;
    private long totalRules;
    private long activeRules;

    // Statistiques des alertes (24h)
    private long totalAlerts24h;
    private long criticalAlerts24h;
    private long highAlerts24h;
    private long mediumAlerts24h;
    private long infoAlerts24h;

    // Alertes non résolues
    private long pendingAlerts;
    private long processingAlerts;
    private long escalatedAlerts;

    // Statistiques par heure (24h)
    private Map<Integer, Long> alertsByHour;

    // Statistiques par type
    private Map<String, Long> alertsByType;

    // Statistiques par caméra
    private Map<String, Long> alertsByCamera;

    // Tendances
    private double alertTrendPercentage; // % vs 24h précédentes
    private double criticalAlertTrendPercentage;

    // Top caméras avec le plus d'alertes
    private List<CameraAlertStats> topCamerasByAlerts;

    // Informations système
    private SystemInfo systemInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CameraAlertStats {
        private String cameraName;
        private long alertCount;
        private long criticalCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemInfo {
        private String version;
        private String uptime;
        private long totalStorageUsed;
        private long totalStorageAvailable;
        private double cpuUsage;
        private double memoryUsage;
    }
}