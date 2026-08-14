package com.banksecurity.backend.dto.response;

import com.banksecurity.backend.model.enums.CameraStatus;
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
public class CameraResponse {

    private UUID id;
    private String name;
    private String rtspUrl;
    private String location;
    private String ipAddress;
    private String model;
    private String manufacturer;
    private CameraStatus status;
    private String resolution;
    private Integer fps;
    private LocalDateTime lastHeartbeat;
    private Boolean isRecording;
    private Boolean isAnalyzing;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer zoneCount;
    private Integer alertCount;
}