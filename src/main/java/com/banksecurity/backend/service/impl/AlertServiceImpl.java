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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
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

        // Associer la caméra
        if (request.getCameraId() != null) {
            Camera camera = cameraRepository.findById(request.getCameraId())
                    .orElseThrow(() -> new ResourceNotFoundException("Caméra", "id", request.getCameraId()));
            alert.setCamera(camera);
        }

        // Associer la zone
        if (request.getZoneId() != null) {
            Zone zone = zoneRepository.findById(request.getZoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", request.getZoneId()));
            alert.setZone(zone);
        }

        // Associer la règle
        if (request.getRuleId() != null) {
            Rule rule = ruleRepository.findById(request.getRuleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Règle", "id", request.getRuleId()));
            alert.setRule(rule);
        }

        alert = alertRepository.save(alert);

        // Journaliser l'action
        auditLogService.logAction(null, "ALERT_CREATED",
                "Alerte créée: " + alert.getType() + " - " + alert.getSeverity());

        // Envoyer l'alerte en temps réel via WebSocket
        AlertResponse response = mapToResponse(alert);
        webSocketService.broadcastAlert(response);

        // Envoyer un email pour les alertes critiques
        if (alert.getSeverity() == AlertSeverity.CRITICAL || alert.getSeverity() == AlertSeverity.HIGH) {
            emailService.sendAlertEmail(alert, null);
        }

        log.info("Alerte créée: {} - {}", alert.getType(), alert.getSeverity());

        return response;
    }

    @Override
    @Transactional
    public AlertResponse updateAlertStatus(UUID id, AlertStatusUpdateRequest request) {
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
    }

    @Override
    public AlertResponse getAlertById(UUID id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alerte", "id", id));
        return mapToResponse(alert);
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
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alerte", "id", id));

        alert.setStatus(AlertStatus.ESCALATED);
        alert = alertRepository.save(alert);

        auditLogService.logAction(null, "ALERT_ESCALATED", "Alerte escaladée: " + id);

        return mapToResponse(alert);
    }

    @Override
    @Transactional
    public AlertResponse resolveAlert(UUID id, AlertStatus resolutionStatus, String notes) {
        if (resolutionStatus != AlertStatus.RESOLVED && resolutionStatus != AlertStatus.FALSE_ALARM) {
            throw new BadRequestException("Le statut de résolution doit être RESOLVED ou FALSE_ALARM");
        }

        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alerte", "id", id));

        alert.setStatus(resolutionStatus);
        alert.setResolvedAt(LocalDateTime.now());
        alert.setResolutionNotes(notes);
        alert = alertRepository.save(alert);

        auditLogService.logAction(null, "ALERT_RESOLVED",
                "Alerte résolue: " + id + " -> " + resolutionStatus);

        return mapToResponse(alert);
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
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerte", "id", alertId));

        // TODO: Sauvegarder l'image et mettre à jour le chemin
        String imagePath = "/storage/images/alerts/" + alertId + ".jpg";
        alert.setImagePath(imagePath);
        alertRepository.save(alert);

        return imagePath;
    }

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