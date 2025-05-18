package com.example.incidentreporter.entity;

import com.example.incidentreporter.enums.IncidentCategory;
import com.example.incidentreporter.enums.IncidentSeverity;
import com.example.incidentreporter.enums.IncidentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Formula;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incidents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    // Ubicación como punto geoespacial
    @Column(columnDefinition = "geometry(Point,4326)")
    private Point location;

    // Coordenadas almacenadas también como double para facilitar acceso
    private double latitude;
    private double longitude;

    // Dirección humana legible
    private String address;

    // Información del incidente
    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    private IncidentSeverity severity;

    @Enumerated(EnumType.STRING)
    private IncidentCategory category;

    // Estado del incidente
    @Enumerated(EnumType.STRING)
    private IncidentStatus status = IncidentStatus.ACTIVE;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;

    // Métricas de confirmación
    private int confirmationCount = 0;
    private int denialCount = 0;
    private int totalNotifications = 0;

    // Radio de afectación (FIJO - no cambia)
    private double radius; // Radio en metros

    // Color del overlay (calculado dinámicamente)
    private double intensityLevel = 0; // 0-100, basado en confirmaciones vs tiempo
    private LocalDateTime lastConfirmationAt;

    // Media attachments (para futuras expansiones)
    @ElementCollection
    private List<String> imageUrls = new ArrayList<>();

    private String audioUrl;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL)
    private List<IncidentConfirmation> confirmations = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        // Por defecto, los incidentes expiran después de 24 horas
        this.expiresAt = LocalDateTime.now().plusHours(24);
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}