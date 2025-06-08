package com.example.incidentreporter.service;

import com.example.incidentreporter.entity.Incident;
import com.example.incidentreporter.entity.User;
import com.example.incidentreporter.repository.UserRepository;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class FCMService {

    // SOLUCIÓN: Inyectar directamente el repository en lugar del service
    private final UserRepository userRepository;

    /**
     * Envía una notificación FCM a un usuario específico
     */
    public String sendIncidentNotification(String fcmToken, Incident incident, double userLatitude, double userLongitude) {
        try {
            double distance = calculateDistance(
                    userLatitude, userLongitude,
                    incident.getLatitude(), incident.getLongitude()
            );

            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(
                            com.google.firebase.messaging.Notification.builder()
                                    .setTitle("⚠️ Incidente Detectado")
                                    .setBody(incident.getTitle() + " - " + (int) distance + "m de tu ubicación")
                                    .build()
                    )
                    .putData("incidentId", incident.getId())
                    .putData("latitude", String.valueOf(incident.getLatitude()))
                    .putData("longitude", String.valueOf(incident.getLongitude()))
                    .putData("distance", String.valueOf(distance))
                    .putData("action", "confirm_incident")
                    .setAndroidConfig(
                            AndroidConfig.builder()
                                    .setPriority(AndroidConfig.Priority.HIGH)
                                    .setNotification(
                                            AndroidNotification.builder()
                                                    .setChannelId("incidents")
                                                    .setIcon("ic_warning")
                                                    .setColor("#FF5722")
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Successfully sent FCM message: {}", response);
            return response;
        } catch (FirebaseMessagingException e) {
            log.error("Error sending FCM message", e);
            throw new RuntimeException("Error sending notification: " + e.getMessage(), e);
        }
    }

    /**
     * Envío masivo de notificaciones a múltiples usuarios
     */
    public BatchResponse sendToMultipleUsers(List<String> tokens, Incident incident) {
        if (tokens.isEmpty()) {
            log.warn("No FCM tokens provided for notification");
            return null;
        }

        try {
            MulticastMessage multicastMessage = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(
                            com.google.firebase.messaging.Notification.builder()
                                    .setTitle("⚠️ Incidente en tu área")
                                    .setBody(incident.getTitle())
                                    .build()
                    )
                    .putData("incidentId", incident.getId())
                    .putData("action", "view_incident")
                    .setAndroidConfig(
                            AndroidConfig.builder()
                                    .setPriority(AndroidConfig.Priority.HIGH)
                                    .build()
                    )
                    .build();

            BatchResponse response = FirebaseMessaging.getInstance().sendMulticast(multicastMessage);
            log.info("Successfully sent {} messages", response.getSuccessCount());

            // Procesar errores para tokens inválidos
            processFailedTokens(tokens, response);

            return response;
        } catch (FirebaseMessagingException e) {
            log.error("Error sending FCM multicast message", e);
            throw new RuntimeException("Error sending multicast notification: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica si un token FCM es válido
     */
    public boolean validateFCMToken(String token) {
        try {
            // Crear un mensaje de prueba con TTL de 0 para no enviarlo realmente
            Message message = Message.builder()
                    .setToken(token)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setDirectBootOk(true)
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setTtl(0) // TTL de 0 para no enviar realmente
                            .build())
                    .build();

            FirebaseMessaging.getInstance().send(message);
            return true;
        } catch (FirebaseMessagingException e) {
            if (e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT ||
                    e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                log.warn("Invalid FCM token detected: {}", e.getMessage());
                return false;
            }
            // Para otros errores, consideramos que el token podría ser válido
            log.warn("FCM validation warning (token may still be valid): {}", e.getMessage());
            return true;
        }
    }

    /**
     * Limpia tokens inválidos - Método público para usar desde UserService
     */
    public void cleanupInvalidTokens() {
        List<User> users = findUsersWithFCMToken();
        for (User user : users) {
            if (user.getFcmToken() != null && !validateFCMToken(user.getFcmToken())) {
                log.info("Cleaning up invalid FCM token for user: {}", user.getEmail());
                user.setFcmToken(null);
                userRepository.save(user);
            }
        }
    }

    /**
     * Encuentra usuarios con token FCM válido
     */
    public List<User> findUsersWithFCMToken() {
        return userRepository.findAll().stream()
                .filter(user -> user.getFcmToken() != null && !user.getFcmToken().trim().isEmpty())
                .toList();
    }

    /**
     * Encuentra usuario por token FCM
     */
    public Optional<User> findByFcmToken(String fcmToken) {
        return userRepository.findByFcmToken(fcmToken);
    }

    /**
     * Procesa tokens fallidos para limpiarlos o reintentar
     */
    private void processFailedTokens(List<String> tokens, BatchResponse response) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            if (!responses.get(i).isSuccessful()) {
                FirebaseMessagingException ex = responses.get(i).getException();
                if (ex.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED ||
                        ex.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                    String invalidToken = tokens.get(i);
                    findByFcmToken(invalidToken).ifPresent(user -> {
                        user.setFcmToken(null);
                        userRepository.save(user);
                        log.info("Removed invalid FCM token for user: {}", user.getEmail());
                    });
                }
            }
        }
    }

    /**
     * Calcula la distancia entre dos puntos usando la fórmula de Haversine
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radio de la Tierra en km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convertir a metros

        return distance;
    }
}