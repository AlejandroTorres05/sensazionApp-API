package com.example.incidentreporter.service;

import com.example.incidentreporter.entity.Incident;
import com.example.incidentreporter.entity.Notification;
import com.example.incidentreporter.entity.User;
import com.example.incidentreporter.enums.NotificationType;
import com.example.incidentreporter.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final FCMService fcmService;

    /**
     * Crea una notificación para un nuevo incidente
     */
    @Transactional
    public Notification createIncidentNotification(User user, Incident incident) {
        Notification notification = Notification.builder()
                .user(user)
                .incident(incident)
                .title("⚠️ Nuevo incidente cerca de ti")
                .message(incident.getTitle())
                .type(NotificationType.NEW_INCIDENT)
                .status(Notification.NotificationStatus.PENDING)
                .build();

        return notificationRepository.save(notification);
    }

    /**
     * Crea una notificación para una actualización de incidente
     */
    @Transactional
    public Notification createIncidentUpdateNotification(User user, Incident incident) {
        Notification notification = Notification.builder()
                .user(user)
                .incident(incident)
                .title("🔄 Actualización de incidente")
                .message("El incidente '" + incident.getTitle() + "' ha sido confirmado por varios usuarios")
                .type(NotificationType.INCIDENT_UPDATE)
                .status(Notification.NotificationStatus.PENDING)
                .build();

        return notificationRepository.save(notification);
    }

    /**
     * Crea una notificación del sistema
     */
    @Transactional
    public Notification createSystemNotification(User user, String title, String message) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(NotificationType.SYSTEM)
                .status(Notification.NotificationStatus.PENDING)
                .build();

        return notificationRepository.save(notification);
    }

    /**
     * Obtiene las notificaciones de un usuario con paginación
     */
    @Transactional(readOnly = true)
    public Page<Notification> getUserNotifications(User user, Pageable pageable) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    /**
     * Marca una notificación como leída
     */
    @Transactional
    public Notification markAsRead(String id, User user) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));

        // Verificar que la notificación pertenece al usuario
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("No autorizado para acceder a esta notificación");
        }

        notification.setStatus(Notification.NotificationStatus.READ);
        notification.setReadAt(LocalDateTime.now());

        return notificationRepository.save(notification);
    }

    /**
     * Envía notificaciones FCM pendientes
     */
    @Scheduled(fixedRate = 60000) // Cada 1 minuto
    @Transactional
    public void sendPendingNotifications() {
        List<Notification> pendingNotifications = notificationRepository.findByStatusAndPushNotificationSent(
                Notification.NotificationStatus.PENDING, false);

        for (Notification notification : pendingNotifications) {
            User user = notification.getUser();

            // Verificar que el usuario tiene token FCM y notificaciones habilitadas
            if (user.getFcmToken() == null || user.getFcmToken().isEmpty() || !user.isNotificationsEnabled()) {
                // Marcar como entregada aunque no se envíe (para evitar reintentos)
                notification.setStatus(Notification.NotificationStatus.DELIVERED);
                notification.setDeliveredAt(LocalDateTime.now());
                notificationRepository.save(notification);
                continue;
            }

            try {
                // Enviar notificación FCM
                if (notification.getIncident() != null) {
                    // Si es de incidente, usar FCM con datos geoespaciales
                    fcmService.sendIncidentNotification(
                            user.getFcmToken(),
                            notification.getIncident(),
                            user.getLocations().isEmpty() ? 0 : user.getLocations().get(0).getLatitude(),
                            user.getLocations().isEmpty() ? 0 : user.getLocations().get(0).getLongitude()
                    );
                } else {
                    // Si es del sistema, usar FCM básico
                    // (aquí podrías implementar otro método en FCMService para notificaciones simples)
                }

                // Marcar como enviada y entregada
                notification.setPushNotificationSent(true);
                notification.setStatus(Notification.NotificationStatus.DELIVERED);
                notification.setDeliveredAt(LocalDateTime.now());
            } catch (Exception e) {
                log.error("Error sending FCM notification", e);
                // No marcar como enviada para reintentar después
            }

            notificationRepository.save(notification);
        }
    }
}