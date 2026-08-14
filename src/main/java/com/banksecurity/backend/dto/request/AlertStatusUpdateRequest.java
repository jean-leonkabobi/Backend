package com.banksecurity.backend.dto.request;

import com.banksecurity.backend.model.enums.AlertStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertStatusUpdateRequest {

    @NotNull(message = "Le statut est obligatoire")
    private AlertStatus status;

    @Size(max = 500, message = "Les notes ne doivent pas dépasser 500 caractères")
    private String resolutionNotes;
}