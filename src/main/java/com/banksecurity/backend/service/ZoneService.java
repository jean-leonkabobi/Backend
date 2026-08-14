package com.banksecurity.backend.service;

import com.banksecurity.backend.dto.request.ZoneRequest;
import com.banksecurity.backend.dto.response.ZoneResponse;
import com.banksecurity.backend.model.enums.ZoneType;

import java.util.List;
import java.util.UUID;

public interface ZoneService {

    /**
     * Crée une nouvelle zone
     */
    ZoneResponse createZone(ZoneRequest request);

    /**
     * Met à jour une zone existante
     */
    ZoneResponse updateZone(UUID id, ZoneRequest request);

    /**
     * Supprime une zone
     */
    void deleteZone(UUID id);

    /**
     * Récupère une zone par son ID
     */
    ZoneResponse getZoneById(UUID id);

    /**
     * Récupère toutes les zones
     */
    List<ZoneResponse> getAllZones();

    /**
     * Récupère les zones par caméra
     */
    List<ZoneResponse> getZonesByCamera(UUID cameraId);

    /**
     * Récupère les zones par type
     */
    List<ZoneResponse> getZonesByType(ZoneType type);

    /**
     * Active ou désactive une zone
     */
    ZoneResponse toggleZoneStatus(UUID id, boolean isActive);

    /**
     * Vérifie si un point est dans une zone
     */
    boolean isPointInZone(UUID zoneId, double x, double y);

    /**
     * Récupère le nombre total de zones
     */
    long countZones();

    /**
     * Récupère les zones actives
     */
    List<ZoneResponse> getActiveZones();
}