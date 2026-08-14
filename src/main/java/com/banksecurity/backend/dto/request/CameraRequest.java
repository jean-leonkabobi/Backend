package com.banksecurity.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CameraRequest {

    @NotBlank(message = "Le nom de la caméra est obligatoire")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    private String name;

    @NotBlank(message = "L'URL RTSP est obligatoire")
    @Pattern(regexp = "^rtsp://.*", message = "L'URL doit commencer par rtsp://")
    private String rtspUrl;

    @Size(max = 255, message = "La localisation ne doit pas dépasser 255 caractères")
    private String location;

    @Pattern(regexp = "^([0-9]{1,3}\\.){3}[0-9]{1,3}$",
            message = "Format d'adresse IP invalide")
    private String ipAddress;

    private String model;
    private String manufacturer;

    @Pattern(regexp = "^\\d{3,4}x\\d{3,4}$",
            message = "Format de résolution invalide (ex: 1920x1080)")
    private String resolution;

    private Integer fps;
}