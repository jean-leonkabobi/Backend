package com.banksecurity.backend.service.impl;

import com.banksecurity.backend.dto.request.AlertRequest;
import com.banksecurity.backend.dto.request.AlertStatusUpdateRequest;
import com.banksecurity.backend.dto.response.AlertResponse;
import com.banksecurity.backend.exception.BadRequestException;
import com.banksecurity.backend.exception.ResourceNotFoundException;
import com.banksecurity.backend.model.Alert;
import com.banksecurity.backend.model.Camera;
import com.banksecurity.backend.model.Rule;
import com.banksecurity.backend.model.Zone;
import com.banksecurity.backend.model.enums.AlertSeverity;
import com.banksecurity.backend.model.enums.AlertStatus;
import com.banksecurity.backend.repository.AlertRepository;
import com.banksecurity.backend.repository.CameraRepository;
import com.banksecurity.backend.repository.RuleRepository;
import com.banksecurity.backend.repository.ZoneRepository;
import com.banksecurity.backend.service.AlertService;
import com.banksecurity.backend.service.AuditLogService;
import com.banksecurity.backend.service.EmailService;
import com.banksecurity.backend.service.WebSocketService;
import com.banksecurity.backend.util.AsyncUtils;
import com.banksecurity.backend.util.Constants;
import com.banksecurity.backend.util.DateUtils;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private final AlertRepository alertRepository;
    private final CameraRepository cameraRepository;
    private final ZoneRepository zoneRepository;
    private final RuleRepository ruleRepository;
    private final AuditLogService auditLogService;
    private final EmailService emailService;
    private final WebSocketService webSocketService;

    @Resource(name = "notificationExecutor")
    private Executor notificationExecutor;

    // ==================== MÉTHODES CRUD EXISTANTES ====================

    @Override
    @Transactional
    public AlertResponse createAlert(AlertRequest request) {
        Alert alert = Alert.builder()
                .type(request.getType())
                .severity(request.getSeverity())
                .status(AlertStatus.PENDING)
                .description(request.getDescription())
                .metadata(request.getMetadata())
                .detectionConfidence(request.getDetectionConfidence())
                .imagePath(request.getImagePath())
                .videoPath(request.getVideoPath())
                .build();

        if (request.getCameraId() != null) {
            Camera camera = cameraRepository.findById(request.getCameraId())
                    .orElseThrow(() -> new ResourceNotFoundException("Caméra", "id", request.getCameraId()));
            alert.setCamera(camera);
        }

        if (request.getZoneId() != null) {
            Zone zone = zoneRepository.findById(request.getZoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", request.getZoneId()));
            alert.setZone(zone);
        }

        if (request.getRuleId() != null) {
            Rule rule = ruleRepository.findById(request.getRuleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Règle", "id", request.getRuleId()));
            alert.setRule(rule);
        }

        alert = alertRepository.save(alert);

        // ✅ Utilisation de Constants.AUDIT_ACTION_ALERT_CREATED
        auditLogService.logAction(null, Constants.AUDIT_ACTION_ALERT_CREATED,
                "Alerte créée: " + alert.getType() + " - " + alert.getSeverity());

        AlertResponse response = mapToResponse(alert);

        // ✅ Utilisation de AsyncUtils.runAsync(Runnable)
        CompletableFuture<Void> futureWebSocket = AsyncUtils.runAsync(
                () -> webSocketService.broadcastAlert(response),
                notificationExecutor,
                "Diffusion alerte " + alert.getId()
        );
        futureWebSocket.exceptionally(e -> {
            log.error("Erreur lors de la diffusion WebSocket: {}", e.getMessage());
            return null;
        });

        // ✅ Création de la variable finale pour la lambda
        final Alert savedAlert = alert;

        if (savedAlert.getSeverity() == AlertSeverity.CRITICAL || savedAlert.getSeverity() == AlertSeverity.HIGH) {
            CompletableFuture<Void> futureEmail = AsyncUtils.runAsync(
                    () -> emailService.sendAlertEmail(savedAlert, null),
                    notificationExecutor,
                    "Envoi email alerte critique " + savedAlert.getId()
            );
            futureEmail.exceptionally(e -> {
                log.error("Erreur lors de l'envoi de l'email: {}", e.getMessage());
                return null;
            });
        }

        log.info("Alerte créée: {} - {}", alert.getType(), alert.getSeverity());

        return response;
    }

    @Override
    @Transactional
    public AlertResponse updateAlertStatus(UUID id, AlertStatusUpdateRequest request) {
        try {
            Alert alert = alertRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Alerte", "id", id));

            alert.setStatus(request.getStatus());

            if (request.getStatus() == AlertStatus.RESOLVED ||
                    request.getStatus() == AlertStatus.FALSE_ALARM) {
                alert.setResolvedAt(LocalDateTime.now());
                alert.setResolutionNotes(request.getResolutionNotes());
            }

            alert = alertRepository.save(alert);

            auditLogService.logAction(null, "ALERT_STATUS_UPDATED",
                    "Alerte " + id + " -> " + request.getStatus());

            return mapToResponse(alert);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du statut de l'alerte: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors de la mise à jour du statut de l'alerte: " + id, e);
        }
    }

    @Override
    public AlertResponse getAlertById(UUID id) {
        try {
            Alert alert = alertRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Alerte", "id", id));
            return mapToResponse(alert);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de l'alerte: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors de la récupération de l'alerte: " + id, e);
        }
    }

    @Override
    public List<AlertResponse> getAllAlerts() {
        return alertRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlertResponse> getAlertsByStatus(AlertStatus status) {
        return alertRepository.findByStatus(status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlertResponse> getAlertsBySeverity(AlertSeverity severity) {
        return alertRepository.findBySeverity(severity).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlertResponse> getAlertsByCamera(UUID cameraId) {
        return alertRepository.findByCameraId(cameraId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlertResponse> getAlertsByZone(UUID zoneId) {
        return alertRepository.findByZoneId(zoneId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlertResponse> getAlertsByDateRange(LocalDateTime start, LocalDateTime end) {
        if (DateUtils.isFuture(start) || DateUtils.isFuture(end)) {
            throw new BadRequestException("Les dates ne peuvent pas être dans le futur");
        }

        return alertRepository.findByCreatedAtBetween(start, end).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlertResponse> getUnresolvedCriticalAlerts() {
        return alertRepository.findUnresolvedCriticalAlerts().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AlertResponse escalateAlert(UUID id) {
        try {
            Alert alert = alertRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Alerte", "id", id));

            alert.setStatus(AlertStatus.ESCALATED);
            alert = alertRepository.save(alert);

            auditLogService.logAction(null, "ALERT_ESCALATED", "Alerte escaladée: " + id);

            return mapToResponse(alert);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de l'escalade de l'alerte: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors de l'escalade de l'alerte: " + id, e);
        }
    }

    @Override
    @Transactional
    public AlertResponse resolveAlert(UUID id, AlertStatus resolutionStatus, String notes) {
        try {
            if (resolutionStatus != AlertStatus.RESOLVED && resolutionStatus != AlertStatus.FALSE_ALARM) {
                throw new BadRequestException("Le statut de résolution doit être RESOLVED ou FALSE_ALARM");
            }

            Alert alert = alertRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Alerte", "id", id));

            alert.setStatus(resolutionStatus);
            alert.setResolvedAt(LocalDateTime.now());
            alert.setResolutionNotes(notes);
            alert = alertRepository.save(alert);

            // ✅ Utilisation de Constants.AUDIT_ACTION_ALERT_RESOLVED
            auditLogService.logAction(null, Constants.AUDIT_ACTION_ALERT_RESOLVED,
                    "Alerte résolue: " + id + " -> " + resolutionStatus);

            return mapToResponse(alert);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la résolution de l'alerte: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors de la résolution de l'alerte: " + id, e);
        }
    }

    @Override
    public long countAlerts() {
        return alertRepository.count();
    }

    @Override
    public long countAlertsByStatus(AlertStatus status) {
        return alertRepository.findByStatus(status).size();
    }

    @Override
    public long countAlertsSince(LocalDateTime since) {
        return alertRepository.countAlertsSince(since);
    }

    @Override
    @Transactional
    public String saveAlertImage(UUID alertId, byte[] imageData) {
        try {
            Alert alert = alertRepository.findById(alertId)
                    .orElseThrow(() -> new ResourceNotFoundException("Alerte", "id", alertId));

            String imagePath = "/storage/images/alerts/" + alertId + ".jpg";
            alert.setImagePath(imagePath);
            alertRepository.save(alert);

            return imagePath;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la sauvegarde de l'image: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors de la sauvegarde de l'image: " + alertId, e);
        }
    }

    // ==================== NOUVELLES MÉTHODES UTILISANT LES REPOSITORY ====================

    public List<AlertResponse> getAlertsByType(String type) {
        return alertRepository.findByType(type).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<AlertResponse> getAlertsByRule(UUID ruleId) {
        return alertRepository.findByRuleId(ruleId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<AlertResponse> getAlertsBySeverityAndStatus(AlertSeverity severity, AlertStatus status) {
        return alertRepository.findBySeverityAndStatus(severity, status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public AlertResponse getAlertWithDetails(UUID id) {
        Alert alert = alertRepository.findAlertWithDetails(id);
        if (alert == null) {
            throw new ResourceNotFoundException("Alerte", "id", id);
        }
        return mapToResponse(alert);
    }

    public List<AlertResponse> getRecentUnprocessedAlerts(int hours) {
        LocalDateTime since = DateUtils.hoursAgo(hours);
        return alertRepository.findRecentUnprocessedAlerts(since).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Page<AlertResponse> getAlertsWithFilters(
            AlertStatus status,
            AlertSeverity severity,
            UUID cameraId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int page,
            int size) {

        if (startDate != null && endDate != null && DateUtils.isFuture(startDate)) {
            throw new BadRequestException("La date de début ne peut pas être dans le futur");
        }

        // ✅ Utilisation de Constants.MAX_ALERTS_PER_PAGE
        int safeSize = Math.min(size, Constants.MAX_ALERTS_PER_PAGE);
        Pageable pageable = PageRequest.of(page, safeSize, Sort.by(Constants.DEFAULT_SORT_FIELD).descending());

        return alertRepository.findAlertsWithFilters(status, severity, cameraId, startDate, endDate, pageable)
                .map(this::mapToResponse);
    }

    public Page<AlertResponse> getAlertsByStatusPaginated(AlertStatus status, int page, int size) {
        // ✅ Utilisation de Constants.MAX_ALERTS_PER_PAGE
        int safeSize = Math.min(size, Constants.MAX_ALERTS_PER_PAGE);
        Pageable pageable = PageRequest.of(page, safeSize, Sort.by(Constants.DEFAULT_SORT_FIELD).descending());
        return alertRepository.findByStatus(status, pageable).map(this::mapToResponse);
    }

    public Page<AlertResponse> getAlertsBySeverityPaginated(AlertSeverity severity, int page, int size) {
        // ✅ Utilisation de Constants.MAX_ALERTS_PER_PAGE
        int safeSize = Math.min(size, Constants.MAX_ALERTS_PER_PAGE);
        Pageable pageable = PageRequest.of(page, safeSize, Sort.by(Constants.DEFAULT_SORT_FIELD).descending());
        return alertRepository.findBySeverity(severity, pageable).map(this::mapToResponse);
    }

    public Page<AlertResponse> getAlertsByCameraPaginated(UUID cameraId, int page, int size) {
        // ✅ Utilisation de Constants.MAX_ALERTS_PER_PAGE
        int safeSize = Math.min(size, Constants.MAX_ALERTS_PER_PAGE);
        Pageable pageable = PageRequest.of(page, safeSize, Sort.by(Constants.DEFAULT_SORT_FIELD).descending());
        return alertRepository.findByCameraId(cameraId, pageable).map(this::mapToResponse);
    }

    // ==================== MÉTHODE DE MAPPING ====================

    private AlertResponse mapToResponse(Alert alert) {
        return AlertResponse.builder()
                .id(alert.getId())
                .cameraId(alert.getCamera() != null ? alert.getCamera().getId() : null)
                .cameraName(alert.getCamera() != null ? alert.getCamera().getName() : null)
                .zoneId(alert.getZone() != null ? alert.getZone().getId() : null)
                .zoneName(alert.getZone() != null ? alert.getZone().getName() : null)
                .ruleId(alert.getRule() != null ? alert.getRule().getId() : null)
                .ruleName(alert.getRule() != null ? alert.getRule().getName() : null)
                .type(alert.getType())
                .severity(alert.getSeverity())
                .status(alert.getStatus())
                .imagePath(alert.getImagePath())
                .videoPath(alert.getVideoPath())
                .description(alert.getDescription())
                .metadata(alert.getMetadata())
                .detectionConfidence(alert.getDetectionConfidence())
                .createdAt(alert.getCreatedAt())
                .resolvedAt(alert.getResolvedAt())
                .resolvedBy(alert.getResolvedBy())
                .resolutionNotes(alert.getResolutionNotes())
                .build();
    }
}