package com.banksecurity.backend.dto.response;

import com.banksecurity.backend.model.enums.AlertSeverity;
import com.banksecurity.backend.model.enums.AlertStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {

    private UUID id;
    private UUID cameraId;
    private String cameraName;
    private UUID zoneId;
    private String zoneName;
    private UUID ruleId;
    private String ruleName;
    private String type;
    private AlertSeverity severity;
    private AlertStatus status;
    private String imagePath;
    private String videoPath;
    private String description;
    private String metadata;
    private Double detectionConfidence;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private UUID resolvedBy;
    private String resolutionNotes;
}