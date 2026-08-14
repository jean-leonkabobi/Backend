package com.banksecurity.backend.dto.request;

import com.banksecurity.backend.model.enums.RuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleRequest {

    @NotBlank(message = "Le nom de la règle est obligatoire")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    private String name;

    private UUID zoneId;

    @NotNull(message = "Le type de règle est obligatoire")
    private RuleType type;

    private String parameters; // JSON string of parameters

    private Integer thresholdTime; // in seconds

    private Integer sensitivity = 50; // 0-100

    private Integer priority = 1;

    @Size(max = 500, message = "La description ne doit pas dépasser 500 caractères")
    private String description;
}