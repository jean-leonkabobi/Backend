package com.banksecurity.backend.service;

import com.banksecurity.backend.dto.request.CameraRequest;
import com.banksecurity.backend.dto.response.CameraResponse;
import com.banksecurity.backend.model.enums.CameraStatus;

import java.util.List;
import java.util.UUID;

public interface CameraService {

    /**
     * Crée une nouvelle caméra
     */
    CameraResponse createCamera(CameraRequest request);

    /**
     * Met à jour une caméra existante
     */
    CameraResponse updateCamera(UUID id, CameraRequest request);

    /**
     * Supprime une caméra
     */
    void deleteCamera(UUID id);

    /**
     * Récupère une caméra par son ID
     */
    CameraResponse getCameraById(UUID id);

    /**
     * Récupère toutes les caméras
     */
    List<CameraResponse> getAllCameras();

    /**
     * Récupère les caméras par statut
     */
    List<CameraResponse> getCamerasByStatus(CameraStatus status);

    /**
     * Démarre l'analyse IA sur une caméra
     */
    void startCameraAnalysis(UUID id);

    /**
     * Arrête l'analyse IA sur une caméra
     */
    void stopCameraAnalysis(UUID id);

    /**
     * Met à jour le statut d'une caméra
     */
    CameraResponse updateCameraStatus(UUID id, CameraStatus status);

    /**
     * Met à jour le heartbeat d'une caméra
     */
    void updateHeartbeat(UUID id);

    /**
     * Vérifie les caméras qui ne répondent pas
     */
    List<CameraResponse> checkUnresponsiveCameras();

    /**
     * Récupère le nombre total de caméras
     */
    long countCameras();

    /**
     * Récupère le nombre de caméras par statut
     */
    long countCamerasByStatus(CameraStatus status);
}