package com.banksecurity.backend.integration.ia;

import com.banksecurity.backend.exception.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Client pour communiquer avec le service IA Python
 */
@Slf4j
@Component
public class IaServiceClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ia.service.url:http://localhost:8000}")
    private String iaServiceUrl;

    @Value("${ia.service.timeout:5000}")
    private int timeout;

    public IaServiceClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Détecte les objets dans une image
     */
    public IaDetectionResponse detectObjects(byte[] imageData, List<String> classesOfInterest) {
        String url = iaServiceUrl + "/detect";

        try {
            IaDetectionRequest request = IaDetectionRequest.builder()
                    .imageBase64(java.util.Base64.getEncoder().encodeToString(imageData))
                    .classesOfInterest(classesOfInterest)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<IaDetectionRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<IaDetectionResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    IaDetectionResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.debug("Détection IA réussie: {} objets détectés",
                        response.getBody().getDetections() != null ?
                                response.getBody().getDetections().size() : 0);
                return response.getBody();
            } else {
                log.error("Réponse inattendue du service IA: {}", response.getStatusCode());
                throw new BadRequestException("Erreur lors de la détection IA");
            }

        } catch (Exception e) {
            log.error("Erreur de communication avec le service IA", e);
            throw new BadRequestException("Service IA indisponible: " + e.getMessage());
        }
    }

    /**
     * Vérifie si le service IA est disponible
     */
    public boolean isServiceAvailable() {
        String url = iaServiceUrl + "/status";

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.warn("Service IA indisponible: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Démarre l'analyse d'une caméra sur le service IA
     */
    public void startCameraAnalysis(String cameraId, String rtspUrl) {
        String url = iaServiceUrl + "/camera/start";

        try {
            Map<String, String> request = Map.of(
                    "camera_id", cameraId,
                    "rtsp_url", rtspUrl
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Analyse IA démarrée pour la caméra: {}", cameraId);
            } else {
                log.error("Erreur lors du démarrage de l'analyse IA: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Erreur de communication avec le service IA", e);
            throw new BadRequestException("Impossible de démarrer l'analyse IA: " + e.getMessage());
        }
    }

    /**
     * Arrête l'analyse d'une caméra sur le service IA
     */
    public void stopCameraAnalysis(String cameraId) {
        String url = iaServiceUrl + "/camera/stop";

        try {
            Map<String, String> request = Map.of("camera_id", cameraId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Analyse IA arrêtée pour la caméra: {}", cameraId);
            }

        } catch (Exception e) {
            log.error("Erreur lors de l'arrêt de l'analyse IA", e);
            throw new BadRequestException("Impossible d'arrêter l'analyse IA: " + e.getMessage());
        }
    }

    /**
     * Récupère le statut du service IA
     */
    public Map<String, Object> getServiceStatus() {
        String url = iaServiceUrl + "/status";

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.warn("Impossible de récupérer le statut du service IA: {}", e.getMessage());
            return Map.of("status", "UNAVAILABLE", "error", e.getMessage());
        }
    }
}