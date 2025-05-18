package com.example.incidentreporter.dto;

import com.example.incidentreporter.enums.ConfirmationAction;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentConfirmationRequest {
    @NotNull(message = "La acción es obligatoria")
    private ConfirmationAction action;

    @NotNull(message = "La latitud es obligatoria")
    private Double userLatitude;

    @NotNull(message = "La longitud es obligatoria")
    private Double userLongitude;

    private String comment;

    @Min(value = 1, message = "La confianza debe ser entre 1 y 5")
    @Max(value = 5, message = "La confianza debe ser entre 1 y 5")
    private Integer confidence = 3; // Valor por defecto

    private Long notificationDelay;

    private String deviceType;
}