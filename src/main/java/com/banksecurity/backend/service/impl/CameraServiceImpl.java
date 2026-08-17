package com.banksecurity.backend.service.impl;

import com.banksecurity.backend.dto.request.CameraRequest;
import com.banksecurity.backend.dto.response.CameraResponse;
import com.banksecurity.backend.exception.BadRequestException;
import com.banksecurity.backend.exception.ConflictException;
import com.banksecurity.backend.exception.ForbiddenException;
import com.banksecurity.backend.exception.ResourceNotFoundException;
import com.banksecurity.backend.model.Camera;
import com.banksecurity.backend.model.enums.CameraStatus;
import com.banksecurity.backend.repository.CameraRepository;
import com.banksecurity.backend.security.UserPrincipal;
import com.banksecurity.backend.service.AuditLogService;
import com.banksecurity.backend.service.CameraService;
import com.banksecurity.backend.util.Constants;
import com.banksecurity.backend.util.DateUtils;
import com.banksecurity.backend.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CameraServiceImpl implements CameraService {

    private final CameraRepository cameraRepository;
    private final AuditLogService auditLogService;

    // ==================== MÉTHODES CRUD EXISTANTES ====================

    @Override
    @Transactional
    public CameraResponse createCamera(CameraRequest request) {
        try {
            // ✅ ValidationUtils.isInvalidRtspUrl (méthode négative)
            if (ValidationUtils.isInvalidRtspUrl(request.getRtspUrl())) {
                throw new BadRequestException("URL RTSP invalide");
            }

            if (request.getIpAddress() != null && !ValidationUtils.isValidIpAddress(request.getIpAddress())) {
                throw new BadRequestException("Adresse IP invalide");
            }

            // ✅ ValidationUtils.isInvalidResolution (méthode négative)
            if (request.getResolution() != null && ValidationUtils.isInvalidResolution(request.getResolution())) {
                throw new BadRequestException("Résolution invalide: " + request.getResolution());
            }

            // ✅ ValidationUtils.isInvalidFps (méthode négative)
            if (request.getFps() != null && ValidationUtils.isInvalidFps(request.getFps())) {
                throw new BadRequestException("FPS invalide (doit être entre 1 et 60): " + request.getFps());
            }

            if (request.getIpAddress() != null && cameraRepository.findByIpAddress(request.getIpAddress()).isPresent()) {
                throw new ConflictException("Une caméra avec cette adresse IP existe déjà: " + request.getIpAddress());
            }

            if (cameraRepository.count() >= Constants.MAX_CAMERAS_PER_SERVER) {
                throw new BadRequestException("Nombre maximum de caméras atteint: " + Constants.MAX_CAMERAS_PER_SERVER);
            }

            Camera camera = Camera.builder()
                    .name(request.getName())
                    .rtspUrl(request.getRtspUrl())
                    .location(request.getLocation())
                    .ipAddress(request.getIpAddress())
                    .model(request.getModel())
                    .manufacturer(request.getManufacturer())
                    .status(CameraStatus.INACTIVE)
                    .resolution(request.getResolution() != null ? request.getResolution() : Constants.DEFAULT_CAMERA_RESOLUTION)
                    .fps(request.getFps() != null ? request.getFps() : Constants.DEFAULT_CAMERA_FPS)
                    .isRecording(false)
                    .isAnalyzing(false)
                    .build();

            camera = cameraRepository.save(camera);

            auditLogService.logAction(null, Constants.AUDIT_ACTION_CREATE + "_CAMERA", "Création caméra: " + camera.getName());
            log.info("Caméra créée: {}", camera.getName());

            return mapToResponse(camera);

        } catch (ConflictException e) {
            throw e;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la création de la caméra: {}", e.getMessage(), e);
            throw new ConflictException("Erreur lors de la création de la caméra: " + request.getName(), e);
        }
    }

    @Override
    @Transactional
    public CameraResponse updateCamera(UUID id, CameraRequest request) {
        try {
            Camera camera = cameraRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Caméra", "id", id));

            // ✅ ValidationUtils.isInvalidRtspUrl (méthode négative)
            if (ValidationUtils.isInvalidRtspUrl(request.getRtspUrl())) {
                throw new BadRequestException("URL RTSP invalide");
            }

            // ✅ ValidationUtils.isInvalidResolution (méthode négative)
            if (request.getResolution() != null && ValidationUtils.isInvalidResolution(request.getResolution())) {
                throw new BadRequestException("Résolution invalide: " + request.getResolution());
            }

            // ✅ ValidationUtils.isInvalidFps (méthode négative)
            if (request.getFps() != null && ValidationUtils.isInvalidFps(request.getFps())) {
                throw new BadRequestException("FPS invalide (doit être entre 1 et 60): " + request.getFps());
            }

            if (request.getIpAddress() != null && !request.getIpAddress().equals(camera.getIpAddress())) {
                cameraRepository.findByIpAddress(request.getIpAddress()).ifPresent(existingCamera -> {
                    throw new ConflictException("Une caméra avec cette adresse IP existe déjà: " + request.getIpAddress());
                });
            }

            camera.setName(request.getName());
            camera.setRtspUrl(request.getRtspUrl());
            camera.setLocation(request.getLocation());
            camera.setIpAddress(request.getIpAddress());
            camera.setModel(request.getModel());
            camera.setManufacturer(request.getManufacturer());

            if (request.getResolution() != null) {
                camera.setResolution(request.getResolution());
            }

            if (request.getFps() != null) {
                camera.setFps(request.getFps());
            }

            camera = cameraRepository.save(camera);

            auditLogService.logAction(null, Constants.AUDIT_ACTION_UPDATE + "_CAMERA", "Mise à jour caméra: " + camera.getName());

            return mapToResponse(camera);

        } catch (ConflictException e) {
            throw e;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour de la caméra: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors de la mise à jour de la caméra: " + request.getName(), e);
        }
    }

    @Override
    @Transactional
    public void deleteCamera(UUID id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new ForbiddenException("Vous n'avez pas les permissions pour supprimer une caméra");
        }

        try {
            Camera camera = cameraRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Caméra", "id", id));

            cameraRepository.delete(camera);

            auditLogService.logAction(null, Constants.AUDIT_ACTION_DELETE + "_CAMERA", "Suppression caméra: " + camera.getName());
            log.info("Caméra supprimée: {}", camera.getName());
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de la caméra: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors de la suppression de la caméra: " + id, e);
        }
    }

    @Override
    public CameraResponse getCameraById(UUID id) {
        try {
            Camera camera = cameraRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Caméra", "id", id));
            return mapToResponse(camera);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de la caméra: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors de la récupération de la caméra: " + id, e);
        }
    }

    @Override
    public List<CameraResponse> getAllCameras() {
        return cameraRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CameraResponse> getCamerasByStatus(CameraStatus status) {
        return cameraRepository.findByStatus(status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void startCameraAnalysis(UUID id) {
        try {
            Camera camera = cameraRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Caméra", "id", id));

            camera.setIsAnalyzing(true);
            camera.setStatus(CameraStatus.ACTIVE);
            cameraRepository.save(camera);

            auditLogService.logAction(null, "START_ANALYSIS", "Démarrage analyse caméra: " + camera.getName());
            log.info("Analyse démarrée pour la caméra: {}", camera.getName());
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors du démarrage de l'analyse: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors du démarrage de l'analyse: " + id, e);
        }
    }

    @Override
    @Transactional
    public void stopCameraAnalysis(UUID id) {
        try {
            Camera camera = cameraRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Caméra", "id", id));

            camera.setIsAnalyzing(false);
            camera.setStatus(CameraStatus.INACTIVE);
            cameraRepository.save(camera);

            auditLogService.logAction(null, "STOP_ANALYSIS", "Arrêt analyse caméra: " + camera.getName());
            log.info("Analyse arrêtée pour la caméra: {}", camera.getName());
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de l'arrêt de l'analyse: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors de l'arrêt de l'analyse: " + id, e);
        }
    }

    @Override
    @Transactional
    public CameraResponse updateCameraStatus(UUID id, CameraStatus status) {
        try {
            Camera camera = cameraRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Caméra", "id", id));

            camera.setStatus(status);
            camera = cameraRepository.save(camera);

            return mapToResponse(camera);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du statut: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors de la mise à jour du statut: " + id, e);
        }
    }

    @Override
    @Transactional
    public void updateHeartbeat(UUID id) {
        try {
            Camera camera = cameraRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Caméra", "id", id));

            camera.setLastHeartbeat(LocalDateTime.now());
            log.debug("Heartbeat reçu à {} pour la caméra {}", DateUtils.formatTime(LocalDateTime.now()), camera.getName());
            if (camera.getStatus() == CameraStatus.ERROR) {
                camera.setStatus(CameraStatus.ACTIVE);
            }
            cameraRepository.save(camera);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du heartbeat: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors de la mise à jour du heartbeat: " + id, e);
        }
    }

    @Override
    public List<CameraResponse> checkUnresponsiveCameras() {
        LocalDateTime timeout = DateUtils.addMinutes(LocalDateTime.now(), -1);
        log.debug("Vérification des caméras sans heartbeat depuis: {}", DateUtils.format(timeout));
        return cameraRepository.findCamerasNotResponding(timeout).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public long countCameras() {
        return cameraRepository.count();
    }

    @Override
    public long countCamerasByStatus(CameraStatus status) {
        return cameraRepository.countByStatus(status);
    }

    // ==================== NOUVELLES MÉTHODES UTILISANT LES REPOSITORY ====================

    public List<CameraResponse> getActiveAnalyzingCameras(CameraStatus status) {
        return cameraRepository.findByStatusAndIsAnalyzingTrue(status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CameraResponse> searchCamerasByLocation(String location) {
        return cameraRepository.findByLocationContainingIgnoreCase(location).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CameraResponse> getCamerasInError() {
        return cameraRepository.findCamerasInError().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public CameraResponse getCameraWithZones(UUID id) {
        Camera camera = cameraRepository.findByIdWithZones(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caméra", "id", id));
        return mapToResponse(camera);
    }

    public List<CameraResponse> getRecordingCameras() {
        return cameraRepository.findByIsRecordingTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CameraResponse> getAnalyzingCameras() {
        return cameraRepository.findByIsAnalyzingTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CameraResponse> searchCamerasByModel(String model) {
        return cameraRepository.findByModelContainingIgnoreCase(model).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CameraResponse> getAllCamerasOrderedByCreation() {
        return cameraRepository.findAllOrderByCreatedAtDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public long countActiveCameras() {
        return cameraRepository.countByStatus(CameraStatus.ACTIVE);
    }

    public long countCamerasInError() {
        return cameraRepository.countByStatus(CameraStatus.ERROR);
    }

    // ==================== MÉTHODE DE MAPPING ====================

    private CameraResponse mapToResponse(Camera camera) {
        return CameraResponse.builder()
                .id(camera.getId())
                .name(camera.getName())
                .rtspUrl(camera.getRtspUrl())
                .location(camera.getLocation())
                .ipAddress(camera.getIpAddress())
                .model(camera.getModel())
                .manufacturer(camera.getManufacturer())
                .status(camera.getStatus())
                .resolution(camera.getResolution())
                .fps(camera.getFps())
                .lastHeartbeat(camera.getLastHeartbeat())
                .isRecording(camera.getIsRecording())
                .isAnalyzing(camera.getIsAnalyzing())
                .createdAt(camera.getCreatedAt())
                .updatedAt(camera.getUpdatedAt())
                .zoneCount(camera.getZones() != null ? camera.getZones().size() : 0)
                .alertCount(camera.getAlerts() != null ? camera.getAlerts().size() : 0)
                .build();
    }
}