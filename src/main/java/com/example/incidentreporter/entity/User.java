package com.example.incidentreporter.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String auth0Id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastActiveAt;

    private boolean profileCompleted;

    // Información adicional del perfil
    private String firstName;
    private String lastName;
    private String phone;
    private String avatarUrl;

    // Configuraciones
    private boolean notificationsEnabled = true;
    private boolean locationSharingEnabled = true;
    private double notificationRadius = 1000; // Radio en metros para recibir notificaciones, por defecto 1km

    // Estadísticas
    private int totalIncidentsReported = 0;
    private int totalConfirmations = 0;
    private double verificationScore = 50.0; // Puntuación inicial de confiabilidad (0-100)

    // FCM Token para notificaciones push
    private String fcmToken;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserLocation> locations = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.lastActiveAt = LocalDateTime.now();
    }
}