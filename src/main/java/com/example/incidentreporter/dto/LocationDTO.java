package com.example.incidentreporter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDTO {
    private String id;
    private String userId;
    private double latitude;
    private double longitude;
    private double accuracy;
    private LocalDateTime timestamp;
    private boolean isActive;
}