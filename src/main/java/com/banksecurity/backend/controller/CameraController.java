package com.banksecurity.backend.controller;

import com.banksecurity.backend.dto.request.CameraRequest;
import com.banksecurity.backend.dto.response.ApiResponse;
import com.banksecurity.backend.dto.response.CameraResponse;
import com.banksecurity.backend.model.enums.CameraStatus;
import com.banksecurity.backend.service.CameraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/cameras")
@RequiredArgsConstructor
@Tag(name = "Caméras", description = "Gestion des caméras IP")
public class CameraController {

    private final CameraService cameraService;

    @GetMapping
    @Operation(summary = "Récupérer toutes les caméras")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SECURITY')")
    public ResponseEntity<ApiResponse<List<CameraResponse>>> getAllCameras() {
        List<CameraResponse> cameras = cameraService.getAllCameras();
        return ResponseEntity.ok(ApiResponse.success("Caméras récupérées", cameras));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer une caméra par ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SECURITY')")
    public ResponseEntity<ApiResponse<CameraResponse>> getCameraById(@PathVariable UUID id) {
        CameraResponse camera = cameraService.getCameraById(id);
        return ResponseEntity.ok(ApiResponse.success("Caméra récupérée", camera));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Récupérer les caméras par statut")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<CameraResponse>>> getCamerasByStatus(@PathVariable CameraStatus status) {
        List<CameraResponse> cameras = cameraService.getCamerasByStatus(status);
        return ResponseEntity.ok(ApiResponse.success("Caméras récupérées", cameras));
    }

    @PostMapping
    @Operation(summary = "Créer une nouvelle caméra")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<CameraResponse>> createCamera(@Valid @RequestBody CameraRequest request) {
        CameraResponse camera = cameraService.createCamera(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Caméra créée", camera));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour une caméra")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<CameraResponse>> updateCamera(@PathVariable UUID id,
                                                                    @Valid @RequestBody CameraRequest request) {
        CameraResponse camera = cameraService.updateCamera(id, request);
        return ResponseEntity.ok(ApiResponse.success("Caméra mise à jour", camera));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une caméra")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCamera(@PathVariable UUID id) {
        cameraService.deleteCamera(id);
        return ResponseEntity.ok(ApiResponse.success("Caméra supprimée", null));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Démarrer l'analyse IA sur une caméra")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> startAnalysis(@PathVariable UUID id) {
        cameraService.startCameraAnalysis(id);
        return ResponseEntity.ok(ApiResponse.success("Analyse démarrée", null));
    }

    @PostMapping("/{id}/stop")
    @Operation(summary = "Arrêter l'analyse IA sur une caméra")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> stopAnalysis(@PathVariable UUID id) {
        cameraService.stopCameraAnalysis(id);
        return ResponseEntity.ok(ApiResponse.success("Analyse arrêtée", null));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Mettre à jour le statut d'une caméra")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<CameraResponse>> updateStatus(@PathVariable UUID id,
                                                                    @RequestParam CameraStatus status) {
        CameraResponse camera = cameraService.updateCameraStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Statut mis à jour", camera));
    }

    @PostMapping("/{id}/heartbeat")
    @Operation(summary = "Mettre à jour le heartbeat d'une caméra")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> heartbeat(@PathVariable UUID id) {
        cameraService.updateHeartbeat(id);
        return ResponseEntity.ok(ApiResponse.success("Heartbeat mis à jour", null));
    }
}