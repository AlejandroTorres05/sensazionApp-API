package com.example.incidentreporter.entity;

import com.example.incidentreporter.enums.ConfirmationAction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "incident_confirmations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentConfirmation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private ConfirmationAction action;

    private LocalDateTime timestamp;

    // Ubicación del usuario cuando realizó la acción
    private double userLatitude;
    private double userLongitude;

    // Información adicional
    private String comment;
    private int confidence; // 1-5, qué tan seguro está el usuario

    // Metadatos
    private long notificationDelay; // Tiempo entre notificación y respuesta (ms)
    private String deviceType;

    @PrePersist
    public void prePersist() {
        this.timestamp = LocalDateTime.now();
    }
}