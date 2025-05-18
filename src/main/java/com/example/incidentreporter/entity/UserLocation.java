package com.example.incidentreporter.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_locations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Ubicación como punto geoespacial
    @Column(columnDefinition = "geometry(Point,4326)")
    private Point location;

    // Coordenadas almacenadas también como double para facilitar acceso
    private double latitude;
    private double longitude;

    // Precisión del GPS en metros
    private double accuracy;

    private LocalDateTime timestamp;

    private boolean isActive = true;
}