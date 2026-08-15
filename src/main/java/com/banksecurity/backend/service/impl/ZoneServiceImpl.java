package com.banksecurity.backend.service.impl;

import com.banksecurity.backend.dto.request.ZoneRequest;
import com.banksecurity.backend.dto.response.ZoneResponse;
import com.banksecurity.backend.exception.BadRequestException;
import com.banksecurity.backend.exception.ConflictException;
import com.banksecurity.backend.exception.ForbiddenException;
import com.banksecurity.backend.exception.ResourceNotFoundException;
import com.banksecurity.backend.model.Camera;
import com.banksecurity.backend.model.Zone;
import com.banksecurity.backend.model.enums.ZoneType;
import com.banksecurity.backend.repository.CameraRepository;
import com.banksecurity.backend.repository.ZoneRepository;
import com.banksecurity.backend.security.UserPrincipal;
import com.banksecurity.backend.service.AuditLogService;
import com.banksecurity.backend.service.ZoneService;
import com.banksecurity.backend.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZoneServiceImpl implements ZoneService {

    private final ZoneRepository zoneRepository;
    private final CameraRepository cameraRepository;
    private final AuditLogService auditLogService;

    // ==================== MÉTHODES CRUD EXISTANTES ====================

    @Override
    @Transactional
    public ZoneResponse createZone(ZoneRequest request) {
        try {
            Camera camera = cameraRepository.findById(request.getCameraId())
                    .orElseThrow(() -> new ResourceNotFoundException("Caméra", "id", request.getCameraId()));

            if (!ValidationUtils.isValidPolygonPoints(request.getPoints())) {
                throw new BadRequestException("Points du polygone invalides. Format attendu: [[x1,y1],[x2,y2],...]");
            }

            if (zoneRepository.existsByCameraIdAndName(request.getCameraId(), request.getName())) {
                throw new ConflictException("Une zone avec ce nom existe déjà pour cette caméra: " + request.getName());
            }

            Zone zone = Zone.builder()
                    .name(request.getName())
                    .camera(camera)
                    .points(request.getPoints())
                    .type(request.getType())
                    .description(request.getDescription())
                    .sensitivity(request.getSensitivity() != null ? request.getSensitivity() : 50)
                    .isActive(true)
                    .build();

            zone = zoneRepository.save(zone);

            auditLogService.logAction(null, "CREATE_ZONE",
                    "Création zone: " + zone.getName() + " pour caméra: " + camera.getName());
            log.info("Zone créée: {}", zone.getName());

            return mapToResponse(zone);

        } catch (ConflictException e) {
            throw e;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la création de la zone: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors de la création de la zone: " + request.getName(), e);
        }
    }

    @Override
    @Transactional
    public ZoneResponse updateZone(UUID id, ZoneRequest request) {
        try {
            Zone zone = zoneRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", id));

            if (request.getCameraId() != null && !request.getCameraId().equals(zone.getCamera().getId())) {
                Camera camera = cameraRepository.findById(request.getCameraId())
                        .orElseThrow(() -> new ResourceNotFoundException("Caméra", "id", request.getCameraId()));
                zone.setCamera(camera);
            }

            if (!zone.getName().equals(request.getName()) &&
                    zoneRepository.existsByCameraIdAndName(request.getCameraId(), request.getName())) {
                throw new ConflictException("Une zone avec ce nom existe déjà pour cette caméra: " + request.getName());
            }

            zone.setName(request.getName());
            zone.setPoints(request.getPoints());
            zone.setType(request.getType());
            zone.setDescription(request.getDescription());

            if (request.getSensitivity() != null) {
                if (!ValidationUtils.isValidSensitivity(request.getSensitivity())) {
                    throw new BadRequestException("La sensibilité doit être entre 0 et 100");
                }
                zone.setSensitivity(request.getSensitivity());
            }

            zone = zoneRepository.save(zone);

            auditLogService.logAction(null, "UPDATE_ZONE", "Mise à jour zone: " + zone.getName());

            return mapToResponse(zone);

        } catch (ConflictException e) {
            throw e;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour de la zone: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors de la mise à jour de la zone: " + request.getName(), e);
        }
    }

    @Override
    @Transactional
    public void deleteZone(UUID id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new ForbiddenException("Vous n'avez pas les permissions pour supprimer une zone");
        }

        try {
            Zone zone = zoneRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", id));

            zoneRepository.delete(zone);

            auditLogService.logAction(null, "DELETE_ZONE", "Suppression zone: " + zone.getName());
            log.info("Zone supprimée: {}", zone.getName());
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de la zone: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors de la suppression de la zone: " + id, e);
        }
    }

    @Override
    public ZoneResponse getZoneById(UUID id) {
        try {
            Zone zone = zoneRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", id));
            return mapToResponse(zone);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de la zone: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors de la récupération de la zone: " + id, e);
        }
    }

    @Override
    public List<ZoneResponse> getAllZones() {
        return zoneRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ZoneResponse> getZonesByCamera(UUID cameraId) {
        return zoneRepository.findByCameraId(cameraId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ZoneResponse> getZonesByType(ZoneType type) {
        return zoneRepository.findByType(type).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ZoneResponse toggleZoneStatus(UUID id, boolean isActive) {
        try {
            Zone zone = zoneRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", id));

            zone.setIsActive(isActive);
            zone = zoneRepository.save(zone);

            auditLogService.logAction(null, "TOGGLE_ZONE_STATUS",
                    "Statut zone " + zone.getName() + " -> " + (isActive ? "active" : "inactive"));

            return mapToResponse(zone);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors du changement de statut de la zone: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors du changement de statut de la zone: " + id, e);
        }
    }

    @Override
    public boolean isPointInZone(UUID zoneId, double x, double y) {
        try {
            Zone zone = zoneRepository.findById(zoneId)
                    .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", zoneId));

            return isPointInPolygon(x, y, parsePoints(zone.getPoints()));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la vérification du point dans la zone: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors de la vérification du point dans la zone: " + zoneId, e);
        }
    }

    @Override
    public long countZones() {
        return zoneRepository.count();
    }

    @Override
    public List<ZoneResponse> getActiveZones() {
        return zoneRepository.findByIsActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ==================== NOUVELLES MÉTHODES UTILISANT LES REPOSITORY ====================

    /**
     * Récupère les zones par caméra et type
     */
    public List<ZoneResponse> getZonesByCameraAndType(UUID cameraId, ZoneType type) {
        return zoneRepository.findByCameraIdAndType(cameraId, type).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère une zone avec ses règles
     */
    public ZoneResponse getZoneWithRules(UUID id) {
        Zone zone = zoneRepository.findByIdWithRules(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", id));
        return mapToResponse(zone);
    }

    /**
     * Récupère une zone avec sa caméra
     */
    public ZoneResponse getZoneWithCamera(UUID id) {
        Zone zone = zoneRepository.findByIdWithCamera(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", id));
        return mapToResponse(zone);
    }

    /**
     * Compte les zones par type
     */
    public long countZonesByType(ZoneType type) {
        return zoneRepository.countByType(type);
    }

    /**
     * Récupère les zones par sensibilité minimale
     */
    public List<ZoneResponse> getZonesBySensitivityGreaterThan(Integer sensitivity) {
        return zoneRepository.findBySensitivityGreaterThanEqual(sensitivity).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les zones inactives
     */
    public List<ZoneResponse> getInactiveZones() {
        return zoneRepository.findByIsActiveFalse().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère toutes les zones triées par date de création
     */
    public List<ZoneResponse> getAllZonesOrderedByCreation() {
        return zoneRepository.findAllOrderByCreatedAtDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Vérifie si une zone existe pour une caméra avec un nom donné
     */
    public boolean zoneExists(UUID cameraId, String name) {
        return zoneRepository.existsByCameraIdAndName(cameraId, name);
    }

    /**
     * Compte les zones actives
     */
    public long countActiveZones() {
        return zoneRepository.findByIsActiveTrue().size();
    }

    /**
     * Compte les zones inactives
     */
    public long countInactiveZones() {
        return zoneRepository.findByIsActiveFalse().size();
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    private boolean isPointInPolygon(double x, double y, double[][] polygon) {
        boolean inside = false;
        int j = polygon.length - 1;

        for (int i = 0; i < polygon.length; i++) {
            if ((polygon[i][1] > y) != (polygon[j][1] > y) &&
                    (x < (polygon[j][0] - polygon[i][0]) * (y - polygon[i][1]) /
                            (polygon[j][1] - polygon[i][1]) + polygon[i][0])) {
                inside = !inside;
            }
            j = i;
        }

        return inside;
    }

    private double[][] parsePoints(String pointsJson) {
        try {
            String[] pointStrings = pointsJson.replaceAll("[\\[\\]]", "").split(",");
            double[][] points = new double[pointStrings.length / 2][2];

            for (int i = 0; i < pointStrings.length; i += 2) {
                points[i / 2][0] = Double.parseDouble(pointStrings[i].trim());
                points[i / 2][1] = Double.parseDouble(pointStrings[i + 1].trim());
            }

            return points;
        } catch (NumberFormatException e) {
            log.error("Erreur de parsing des points: {}", pointsJson, e);
            throw new BadRequestException("Format de points invalide: " + pointsJson);
        }
    }

    private ZoneResponse mapToResponse(Zone zone) {
        return ZoneResponse.builder()
                .id(zone.getId())
                .name(zone.getName())
                .cameraId(zone.getCamera() != null ? zone.getCamera().getId() : null)
                .cameraName(zone.getCamera() != null ? zone.getCamera().getName() : null)
                .points(zone.getPoints())
                .type(zone.getType())
                .isActive(zone.getIsActive())
                .description(zone.getDescription())
                .sensitivity(zone.getSensitivity())
                .createdAt(zone.getCreatedAt())
                .updatedAt(zone.getUpdatedAt())
                .ruleCount(zone.getRules() != null ? zone.getRules().size() : 0)
                .build();
    }
}