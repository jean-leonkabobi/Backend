package com.banksecurity.backend.controller;

import com.banksecurity.backend.dto.request.ZoneRequest;
import com.banksecurity.backend.dto.response.ApiResponse;
import com.banksecurity.backend.dto.response.ZoneResponse;
import com.banksecurity.backend.model.enums.ZoneType;
import com.banksecurity.backend.service.ZoneService;
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
@RequestMapping("/zones")
@RequiredArgsConstructor
@Tag(name = "Zones", description = "Gestion des zones virtuelles")
public class ZoneController {

    private final ZoneService zoneService;

    @GetMapping
    @Operation(summary = "Récupérer toutes les zones")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SECURITY')")
    public ResponseEntity<ApiResponse<List<ZoneResponse>>> getAllZones() {
        List<ZoneResponse> zones = zoneService.getAllZones();
        return ResponseEntity.ok(ApiResponse.success("Zones récupérées", zones));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer une zone par ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SECURITY')")
    public ResponseEntity<ApiResponse<ZoneResponse>> getZoneById(@PathVariable UUID id) {
        ZoneResponse zone = zoneService.getZoneById(id);
        return ResponseEntity.ok(ApiResponse.success("Zone récupérée", zone));
    }

    @GetMapping("/camera/{cameraId}")
    @Operation(summary = "Récupérer les zones d'une caméra")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SECURITY')")
    public ResponseEntity<ApiResponse<List<ZoneResponse>>> getZonesByCamera(@PathVariable UUID cameraId) {
        List<ZoneResponse> zones = zoneService.getZonesByCamera(cameraId);
        return ResponseEntity.ok(ApiResponse.success("Zones récupérées", zones));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Récupérer les zones par type")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<ZoneResponse>>> getZonesByType(@PathVariable ZoneType type) {
        List<ZoneResponse> zones = zoneService.getZonesByType(type);
        return ResponseEntity.ok(ApiResponse.success("Zones récupérées", zones));
    }

    @PostMapping
    @Operation(summary = "Créer une nouvelle zone")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ZoneResponse>> createZone(@Valid @RequestBody ZoneRequest request) {
        ZoneResponse zone = zoneService.createZone(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Zone créée", zone));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour une zone")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ZoneResponse>> updateZone(@PathVariable UUID id,
                                                                @Valid @RequestBody ZoneRequest request) {
        ZoneResponse zone = zoneService.updateZone(id, request);
        return ResponseEntity.ok(ApiResponse.success("Zone mise à jour", zone));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une zone")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteZone(@PathVariable UUID id) {
        zoneService.deleteZone(id);
        return ResponseEntity.ok(ApiResponse.success("Zone supprimée", null));
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "Activer/désactiver une zone")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ZoneResponse>> toggleZone(@PathVariable UUID id,
                                                                @RequestParam boolean isActive) {
        ZoneResponse zone = zoneService.toggleZoneStatus(id, isActive);
        return ResponseEntity.ok(ApiResponse.success("Statut de la zone mis à jour", zone));
    }
}