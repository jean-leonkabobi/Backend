package com.banksecurity.backend.service.impl;

import com.banksecurity.backend.dto.response.DashboardStatsResponse;
import com.banksecurity.backend.model.enums.AlertSeverity;
import com.banksecurity.backend.model.enums.AlertStatus;
import com.banksecurity.backend.model.enums.CameraStatus;
import com.banksecurity.backend.repository.AlertRepository;
import com.banksecurity.backend.repository.CameraRepository;
import com.banksecurity.backend.repository.RuleRepository;
import com.banksecurity.backend.repository.ZoneRepository;
import com.banksecurity.backend.service.DashboardService;
import com.banksecurity.backend.util.AsyncUtils;
import com.banksecurity.backend.util.Constants;
import com.banksecurity.backend.util.DateUtils;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AlertRepository alertRepository;
    private final CameraRepository cameraRepository;
    private final ZoneRepository zoneRepository;
    private final RuleRepository ruleRepository;

    @Resource(name = "taskExecutor")
    private Executor taskExecutor;

    @Override
    public DashboardStatsResponse getGlobalStats() {
        LocalDateTime last24h = DateUtils.hoursAgo(24);
        LocalDateTime previous24h = DateUtils.hoursAgo(48);

        long totalCameras = cameraRepository.count();
        long activeCameras = cameraRepository.countByStatus(CameraStatus.ACTIVE);
        long camerasInError = cameraRepository.countByStatus(CameraStatus.ERROR);
        long totalZones = zoneRepository.count();
        long totalRules = ruleRepository.count();
        long activeRules = ruleRepository.findByIsActiveTrue().size();

        long totalAlerts24h = alertRepository.countAlertsSince(last24h);
        long criticalAlerts24h = alertRepository.countAlertsBySeveritySince(AlertSeverity.CRITICAL, last24h);
        long highAlerts24h = alertRepository.countAlertsBySeveritySince(AlertSeverity.HIGH, last24h);
        long mediumAlerts24h = alertRepository.countAlertsBySeveritySince(AlertSeverity.MEDIUM, last24h);
        long infoAlerts24h = alertRepository.countAlertsBySeveritySince(AlertSeverity.INFO, last24h);

        long pendingAlerts = alertRepository.countAlertsByStatusSince(AlertStatus.PENDING, last24h);
        long processingAlerts = alertRepository.countAlertsByStatusSince(AlertStatus.PROCESSING, last24h);
        long escalatedAlerts = alertRepository.countAlertsByStatusSince(AlertStatus.ESCALATED, last24h);

        CompletableFuture<Map<?, Long>> alertsByHourFuture = AsyncUtils.runAsync(
                () -> convertToIntMap(alertRepository.countAlertsByHourSince(last24h)),
                taskExecutor,
                "Statistiques alertes par heure"
        );

        CompletableFuture<Map<?, Long>> alertsByTypeFuture = AsyncUtils.runAsync(
                () -> convertToStringMap(alertRepository.countAlertsByTypeSince(last24h)),
                taskExecutor,
                "Statistiques alertes par type"
        );

        CompletableFuture<Map<?, Long>> alertsByCameraFuture = AsyncUtils.runAsync(
                () -> convertToStringMap(alertRepository.countAlertsByCameraSince(last24h)),
                taskExecutor,
                "Statistiques alertes par caméra"
        );

        List<CompletableFuture<Map<?, Long>>> futures = new ArrayList<>();
        futures.add(alertsByHourFuture);
        futures.add(alertsByTypeFuture);
        futures.add(alertsByCameraFuture);

        List<Map<?, Long>> parallelResults = AsyncUtils.allOf(futures).join();

        @SuppressWarnings("unchecked")
        Map<Integer, Long> alertsByHour = (Map<Integer, Long>) parallelResults.get(0);

        @SuppressWarnings("unchecked")
        Map<String, Long> alertsByType = (Map<String, Long>) parallelResults.get(1);

        @SuppressWarnings("unchecked")
        Map<String, Long> alertsByCamera = (Map<String, Long>) parallelResults.get(2);

        long previousTotalAlerts = alertRepository.countAlertsSince(previous24h) - totalAlerts24h;
        double alertTrend = calculateTrendPercentage(totalAlerts24h, previousTotalAlerts);

        long previousCriticalAlerts = alertRepository.countAlertsBySeveritySince(AlertSeverity.CRITICAL, previous24h) - criticalAlerts24h;
        double criticalTrend = calculateTrendPercentage(criticalAlerts24h, previousCriticalAlerts);

        List<DashboardStatsResponse.CameraAlertStats> topCameras = getTopCamerasByAlerts(5).entrySet().stream()
                .map(entry -> {
                    String cameraName = cameraRepository.findById(entry.getKey())
                            .map(camera -> camera.getName())
                            .orElse("Caméra inconnue");

                    return DashboardStatsResponse.CameraAlertStats.builder()
                            .cameraName(cameraName)
                            .alertCount(entry.getValue())
                            .criticalCount(alertRepository.countAlertsBySeveritySince(AlertSeverity.CRITICAL, last24h))
                            .build();
                })
                .collect(Collectors.toList());

        DashboardStatsResponse.SystemInfo systemInfo = getSystemInfo();

        return DashboardStatsResponse.builder()
                .totalCameras(totalCameras)
                .activeCameras(activeCameras)
                .camerasInError(camerasInError)
                .totalZones(totalZones)
                .totalRules(totalRules)
                .activeRules(activeRules)
                .totalAlerts24h(totalAlerts24h)
                .criticalAlerts24h(criticalAlerts24h)
                .highAlerts24h(highAlerts24h)
                .mediumAlerts24h(mediumAlerts24h)
                .infoAlerts24h(infoAlerts24h)
                .pendingAlerts(pendingAlerts)
                .processingAlerts(processingAlerts)
                .escalatedAlerts(escalatedAlerts)
                .alertsByHour(alertsByHour)
                .alertsByType(alertsByType)
                .alertsByCamera(alertsByCamera)
                .alertTrendPercentage(alertTrend)
                .criticalAlertTrendPercentage(criticalTrend)
                .topCamerasByAlerts(topCameras)
                .systemInfo(systemInfo)
                .build();
    }

    @Override
    public DashboardStatsResponse getStatsForPeriod(LocalDateTime start, LocalDateTime end) {
        long totalAlerts = alertRepository.findByCreatedAtBetween(start, end).size();

        return DashboardStatsResponse.builder()
                .totalAlerts24h(totalAlerts)
                .criticalAlerts24h(alertRepository.countAlertsBySeveritySince(AlertSeverity.CRITICAL, start))
                .highAlerts24h(alertRepository.countAlertsBySeveritySince(AlertSeverity.HIGH, start))
                .mediumAlerts24h(alertRepository.countAlertsBySeveritySince(AlertSeverity.MEDIUM, start))
                .infoAlerts24h(alertRepository.countAlertsBySeveritySince(AlertSeverity.INFO, start))
                .build();
    }

    @Override
    public Map<Integer, Long> getAlertsByHour() {
        LocalDateTime last24h = DateUtils.hoursAgo(24);
        return convertToIntMap(alertRepository.countAlertsByHourSince(last24h));
    }

    @Override
    public Map<String, Long> getAlertsByType() {
        LocalDateTime last24h = DateUtils.hoursAgo(24);
        return convertToStringMap(alertRepository.countAlertsByTypeSince(last24h));
    }

    @Override
    public Map<String, Long> getAlertsByCamera() {
        LocalDateTime last24h = DateUtils.hoursAgo(24);
        return convertToStringMap(alertRepository.countAlertsByCameraSince(last24h));
    }

    @Override
    public Map<UUID, Long> getTopCamerasByAlerts(int limit) {
        LocalDateTime last24h = DateUtils.hoursAgo(24);
        List<Object[]> results = alertRepository.countAlertsByCameraSince(last24h);

        Map<UUID, Long> topCameras = new HashMap<>();

        for (Object[] result : results) {
            if (result.length >= 2 && result[0] != null && result[1] != null) {
                String cameraName = result[0].toString();
                long alertCount = Long.parseLong(result[1].toString());

                cameraRepository.findAll().stream()
                        .filter(camera -> camera.getName().equals(cameraName))
                        .findFirst()
                        .ifPresent(camera -> topCameras.put(camera.getId(), alertCount));
            }
        }

        return topCameras;
    }

    @Override
    public double calculateAlertTrend() {
        LocalDateTime last24h = DateUtils.hoursAgo(24);
        LocalDateTime previous24h = DateUtils.hoursAgo(48);

        long currentAlerts = alertRepository.countAlertsSince(last24h);
        long previousAlerts = alertRepository.countAlertsSince(previous24h) - currentAlerts;

        return calculateTrendPercentage(currentAlerts, previousAlerts);
    }

    @Override
    public DashboardStatsResponse.SystemInfo getSystemInfo() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long usedMemory = totalMemory - freeMemory;

        double cpuUsage = osBean.getSystemLoadAverage();
        double memoryUsage = (double) usedMemory / totalMemory * 100;

        return DashboardStatsResponse.SystemInfo.builder()
                // ✅ Utilisation de Constants.APP_VERSION
                .version(Constants.APP_VERSION)
                .uptime(ManagementFactory.getRuntimeMXBean().getUptime() + " ms")
                .totalStorageUsed(usedMemory)
                .totalStorageAvailable(totalMemory)
                .cpuUsage(cpuUsage)
                .memoryUsage(memoryUsage)
                .build();
    }

    @Override
    public long getPendingAlertsCount() {
        return alertRepository.findByStatus(AlertStatus.PENDING).size();
    }

    @Override
    public long getEscalatedAlertsCount() {
        return alertRepository.findByStatus(AlertStatus.ESCALATED).size();
    }

    private Map<String, Long> convertToStringMap(List<Object[]> results) {
        Map<String, Long> map = new HashMap<>();
        for (Object[] result : results) {
            if (result.length >= 2 && result[0] != null && result[1] != null) {
                map.put(result[0].toString(), Long.parseLong(result[1].toString()));
            }
        }
        return map;
    }

    private Map<Integer, Long> convertToIntMap(List<Object[]> results) {
        Map<Integer, Long> map = new HashMap<>();
        for (Object[] result : results) {
            if (result.length >= 2 && result[0] != null && result[1] != null) {
                try {
                    map.put(Integer.parseInt(result[0].toString()), Long.parseLong(result[1].toString()));
                } catch (NumberFormatException e) {
                    log.warn("Impossible de convertir en Integer: {}", result[0]);
                }
            }
        }
        return map;
    }

    private double calculateTrendPercentage(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return ((double) (current - previous) / previous) * 100;
    }
}