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
public class UserDTO {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String avatarUrl;
    private boolean notificationsEnabled;
    private boolean locationSharingEnabled;
    private double notificationRadius;
    private int totalIncidentsReported;
    private int totalConfirmations;
    private double verificationScore;
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;
}