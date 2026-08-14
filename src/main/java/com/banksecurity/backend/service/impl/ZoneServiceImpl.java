package com.banksecurity.backend.service.impl;

import com.banksecurity.backend.dto.request.ZoneRequest;
import com.banksecurity.backend.dto.response.ZoneResponse;
import com.banksecurity.backend.exception.BadRequestException;
import com.banksecurity.backend.exception.ConflictException;
import com.banksecurity.backend.exception.ResourceNotFoundException;
import com.banksecurity.backend.model.Camera;
import com.banksecurity.backend.model.Zone;
import com.banksecurity.backend.model.enums.ZoneType;
import com.banksecurity.backend.repository.CameraRepository;
import com.banksecurity.backend.repository.ZoneRepository;
import com.banksecurity.backend.service.AuditLogService;
import com.banksecurity.backend.service.ZoneService;
import com.banksecurity.backend.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZoneServiceImpl implements ZoneService {

    private final ZoneRepository zoneRepository;
    private final CameraRepository cameraRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public ZoneResponse createZone(ZoneRequest request) {
        // Vérifier que la caméra existe
        Camera camera = cameraRepository.findById(request.getCameraId())
                .orElseThrow(() -> new ResourceNotFoundException("Caméra", "id", request.getCameraId()));

        // Valider les points du polygone
        if (!ValidationUtils.isValidPolygonPoints(request.getPoints())) {
            throw new BadRequestException("Points du polygone invalides. Format attendu: [[x1,y1],[x2,y2],...]");
        }

        // ✅ Utilisation de ConflictException
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
    }

    @Override
    @Transactional
    public ZoneResponse updateZone(UUID id, ZoneRequest request) {
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", id));

        if (request.getCameraId() != null && !request.getCameraId().equals(zone.getCamera().getId())) {
            Camera camera = cameraRepository.findById(request.getCameraId())
                    .orElseThrow(() -> new ResourceNotFoundException("Caméra", "id", request.getCameraId()));
            zone.setCamera(camera);
        }

        // ✅ Utilisation de ConflictException
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
    }

    @Override
    @Transactional
    public void deleteZone(UUID id) {
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", id));

        zoneRepository.delete(zone);

        auditLogService.logAction(null, "DELETE_ZONE", "Suppression zone: " + zone.getName());
        log.info("Zone supprimée: {}", zone.getName());
    }

    @Override
    public ZoneResponse getZoneById(UUID id) {
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", id));
        return mapToResponse(zone);
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
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", id));

        zone.setIsActive(isActive);
        zone = zoneRepository.save(zone);

        auditLogService.logAction(null, "TOGGLE_ZONE_STATUS",
                "Statut zone " + zone.getName() + " -> " + (isActive ? "active" : "inactive"));

        return mapToResponse(zone);
    }

    @Override
    public boolean isPointInZone(UUID zoneId, double x, double y) {
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone", "id", zoneId));

        // Implémentation de l'algorithme Point-in-Polygon
        return isPointInPolygon(x, y, parsePoints(zone.getPoints()));
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

    /**
     * Algorithme Point-in-Polygon (Ray Casting)
     */
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

    /**
     * Parse les points du polygone depuis la chaîne JSON
     */
    private double[][] parsePoints(String pointsJson) {
        try {
            // Format: "[[x1,y1],[x2,y2],...]"
            String[] pointStrings = pointsJson.replaceAll("[\\[\\]]", "").split(",");
            double[][] points = new double[pointStrings.length / 2][2];

            for (int i = 0; i < pointStrings.length; i += 2) {
                points[i / 2][0] = Double.parseDouble(pointStrings[i].trim());
                points[i / 2][1] = Double.parseDouble(pointStrings[i + 1].trim());
            }

            return points;
        } catch (Exception e) {
            log.error("Erreur lors du parsing des points: {}", e.getMessage());
            return new double[0][0];
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