package com.banksecurity.backend.service.impl;

import com.banksecurity.backend.dto.request.CameraRequest;
import com.banksecurity.backend.dto.response.CameraResponse;
import com.banksecurity.backend.exception.BadRequestException;
import com.banksecurity.backend.exception.ResourceNotFoundException;
import com.banksecurity.backend.model.Camera;
import com.banksecurity.backend.model.enums.CameraStatus;
import com.banksecurity.backend.repository.CameraRepository;
import com.banksecurity.backend.service.AuditLogService;
import com.banksecurity.backend.service.CameraService;
import com.banksecurity.backend.util.ValidationUtils;
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
public class CameraServiceImpl implements CameraService {

    private final CameraRepository cameraRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public CameraResponse createCamera(CameraRequest request) {
        // Valider l'URL RTSP
        if (!ValidationUtils.isValidRtspUrl(request.getRtspUrl())) {
            throw new BadRequestException("URL RTSP invalide");
        }

        // Valider l'adresse IP si fournie
        if (request.getIpAddress() != null && !ValidationUtils.isValidIpAddress(request.getIpAddress())) {
            throw new BadRequestException("Adresse IP invalide");
        }

        Camera camera = Camera.builder()
                .name(request.getName())
                .rtspUrl(request.getRtspUrl())
                .location(request.getLocation())
                .ipAddress(request.getIpAddress())
                .model(request.getModel())
                .manufacturer(request.getManufacturer())
                .status(CameraStatus.INACTIVE)
                .resolution(request.getResolution() != null ? request.getResolution() : "1920x1080")
                .fps(request.getFps() != null ? request.getFps() : 15)
                .isRecording(false)
                .isAnalyzing(false)
                .build();

        camera = cameraRepository.save(camera);

        auditLogService.logAction(null, "CREATE_CAMERA", "Création caméra: " + camera.getName());
        log.info("Caméra créée: {}", camera.getName());

        return mapToResponse(camera);
    }

    @Override
    @Transactional
    public CameraResponse updateCamera(UUID id, CameraRequest request) {
        Camera camera = cameraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caméra", "id", id));

        if (!ValidationUtils.isValidRtspUrl(request.getRtspUrl())) {
            throw new BadRequestException("URL RTSP invalide");
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

        auditLogService.logAction(null, "UPDATE_CAMERA", "Mise à jour caméra: " + camera.getName());

        return mapToResponse(camera);
    }

    @Override
    @Transactional
    public void deleteCamera(UUID id) {
        Camera camera = cameraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caméra", "id", id));

        cameraRepository.delete(camera);

        auditLogService.logAction(null, "DELETE_CAMERA", "Suppression caméra: " + camera.getName());
        log.info("Caméra supprimée: {}", camera.getName());
    }

    @Override
    public CameraResponse getCameraById(UUID id) {
        Camera camera = cameraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caméra", "id", id));
        return mapToResponse(camera);
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
        Camera camera = cameraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caméra", "id", id));

        camera.setIsAnalyzing(true);
        camera.setStatus(CameraStatus.ACTIVE);
        cameraRepository.save(camera);

        auditLogService.logAction(null, "START_ANALYSIS", "Démarrage analyse caméra: " + camera.getName());
        log.info("Analyse démarrée pour la caméra: {}", camera.getName());
    }

    @Override
    @Transactional
    public void stopCameraAnalysis(UUID id) {
        Camera camera = cameraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caméra", "id", id));

        camera.setIsAnalyzing(false);
        camera.setStatus(CameraStatus.INACTIVE);
        cameraRepository.save(camera);

        auditLogService.logAction(null, "STOP_ANALYSIS", "Arrêt analyse caméra: " + camera.getName());
        log.info("Analyse arrêtée pour la caméra: {}", camera.getName());
    }

    @Override
    @Transactional
    public CameraResponse updateCameraStatus(UUID id, CameraStatus status) {
        Camera camera = cameraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caméra", "id", id));

        camera.setStatus(status);
        camera = cameraRepository.save(camera);

        return mapToResponse(camera);
    }

    @Override
    @Transactional
    public void updateHeartbeat(UUID id) {
        Camera camera = cameraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caméra", "id", id));

        camera.setLastHeartbeat(LocalDateTime.now());
        if (camera.getStatus() == CameraStatus.ERROR) {
            camera.setStatus(CameraStatus.ACTIVE);
        }
        cameraRepository.save(camera);
    }

    @Override
    public List<CameraResponse> checkUnresponsiveCameras() {
        LocalDateTime timeout = LocalDateTime.now().minusSeconds(30);
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