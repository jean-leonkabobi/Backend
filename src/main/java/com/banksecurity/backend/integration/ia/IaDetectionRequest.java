package com.banksecurity.backend.integration.ia;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Requête de détection IA
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IaDetectionRequest {

    /**
     * Image encodée en base64
     */
    private String imageBase64;

    /**
     * Classes d'objets à détecter (optionnel)
     */
    private List<String> classesOfInterest;

    /**
     * Seuil de confiance (optionnel, 0.0 à 1.0)
     */
    private Double confidenceThreshold;
}