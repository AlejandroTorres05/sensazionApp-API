package com.example.incidentreporter.dto;

import com.example.incidentreporter.enums.IncidentCategory;
import com.example.incidentreporter.enums.IncidentSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentRequest {
    @NotNull(message = "La latitude es obligatoria")
    private Double latitude;

    @NotNull(message = "La longitude es obligatoria")
    private Double longitude;

    private String address;

    @NotBlank(message = "El título es obligatorio")
    private String title;

    private String description;

    @NotNull(message = "La severidad es obligatoria")
    private IncidentSeverity severity;

    @NotNull(message = "La categoría es obligatoria")
    private IncidentCategory category;

    @NotNull(message = "El radio es obligatorio")
    private Double radius;

    private List<String> imageUrls;

    private String audioUrl;
}