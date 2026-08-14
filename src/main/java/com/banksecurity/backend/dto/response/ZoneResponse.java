package com.banksecurity.backend.dto.response;

import com.banksecurity.backend.model.enums.ZoneType;
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
public class ZoneResponse {

    private UUID id;
    private String name;
    private UUID cameraId;
    private String cameraName;
    private String points;
    private ZoneType type;
    private Boolean isActive;
    private String description;
    private Integer sensitivity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer ruleCount;
}