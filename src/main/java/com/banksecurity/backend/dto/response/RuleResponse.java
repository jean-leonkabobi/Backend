package com.banksecurity.backend.dto.response;

import com.banksecurity.backend.model.enums.RuleType;
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
public class RuleResponse {

    private UUID id;
    private String name;
    private UUID zoneId;
    private String zoneName;
    private RuleType type;
    private String parameters;
    private Integer thresholdTime;
    private Integer sensitivity;
    private Integer priority;
    private Boolean isActive;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}