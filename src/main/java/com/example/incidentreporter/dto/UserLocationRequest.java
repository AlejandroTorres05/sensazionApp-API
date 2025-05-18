package com.example.incidentreporter.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLocationRequest {
    @NotNull(message = "La latitud es obligatoria")
    private Double latitude;

    @NotNull(message = "La longitud es obligatoria")
    private Double longitude;

    private Double accuracy;
}