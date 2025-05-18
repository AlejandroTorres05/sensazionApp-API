package com.example.incidentreporter.dto;

import com.example.incidentreporter.enums.IncidentCategory;
import com.example.incidentreporter.enums.IncidentSeverity;
import com.example.incidentreporter.enums.IncidentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentDTO {
    private String id;
    private String reporterId;
    private String reporterEmail; // Para facilitar visualización
    private double latitude;
    private double longitude;
    private String address;
    private String title;
    private String description;
    private IncidentSeverity severity;
    private IncidentCategory category;
    private IncidentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
    private int confirmationCount;
    private int denialCount;
    private double radius;
    private double intensityLevel;
    private LocalDateTime lastConfirmationAt;
    private List<String> imageUrls;
    private String audioUrl;
    private double distance; // Distancia al usuario (opcional, para queries de cercanía)
    private boolean userHasConfirmed; // Si el usuario actual ya interactuó con este incidente
}