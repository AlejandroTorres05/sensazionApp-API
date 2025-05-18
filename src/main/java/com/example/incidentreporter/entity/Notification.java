package com.example.incidentreporter.entity;

import com.example.incidentreporter.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    // Contenido de la notificación
    private String title;
    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    // Estado
    @Enumerated(EnumType.STRING)
    private NotificationStatus status = NotificationStatus.PENDING;

    private LocalDateTime createdAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;

    // Push notification
    private boolean pushNotificationSent = false;
    private String pushNotificationId;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public enum NotificationStatus {
        PENDING, DELIVERED, READ, RESPONDED
    }
}