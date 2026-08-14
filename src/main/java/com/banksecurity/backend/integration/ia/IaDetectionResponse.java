package com.banksecurity.backend.integration.ia;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Réponse de détection IA
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IaDetectionResponse {

    /**
     * Liste des détections
     */
    private List<Detection> detections;

    /**
     * Horodatage de la détection
     */
    private String timestamp;

    /**
     * Temps de traitement en millisecondes
     */
    private Double processingTimeMs;

    /**
     * Objet détecté
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Detection {

        /**
         * Classe de l'objet détecté
         */
        private String className;

        /**
         * Confiance de la détection (0.0 à 1.0)
         */
        private Double confidence;

        /**
         * Coordonnées de la boîte englobante [x1, y1, x2, y2]
         */
        private List<Double> bbox;

        /**
         * Centre de la boîte [cx, cy]
         */
        private List<Double> center;

        /**
         * Surface de la boîte englobante
         */
        private Double area;
    }
}