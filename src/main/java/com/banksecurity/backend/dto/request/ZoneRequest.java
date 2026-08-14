package com.banksecurity.backend.dto.request;

import com.banksecurity.backend.model.enums.ZoneType;
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
public class ZoneRequest {

    @NotBlank(message = "Le nom de la zone est obligatoire")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    private String name;

    @NotNull(message = "L'ID de la caméra est obligatoire")
    private UUID cameraId;

    @NotBlank(message = "Les points du polygone sont obligatoires")
    private String points; // JSON string of polygon points

    @NotNull(message = "Le type de zone est obligatoire")
    private ZoneType type;

    @Size(max = 500, message = "La description ne doit pas dépasser 500 caractères")
    private String description;

    private Integer sensitivity = 50; // 0-100
}