package com.banksecurity.backend.dto.request;

import com.banksecurity.backend.model.enums.AlertSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRequest {

    @NotNull(message = "L'ID de la caméra est obligatoire")
    private UUID cameraId;

    private UUID zoneId;
    private UUID ruleId;

    @NotBlank(message = "Le type d'alerte est obligatoire")
    private String type;

    @NotNull(message = "La sévérité est obligatoire")
    private AlertSeverity severity;

    private String imagePath;
    private String videoPath;

    private String description;
    private String metadata; // JSON string

    private Double detectionConfidence;
}