package com.example.incidentreporter.service;

import com.example.incidentreporter.dto.IncidentConfirmationRequest;
import com.example.incidentreporter.dto.IncidentDTO;
import com.example.incidentreporter.dto.IncidentRequest;
import com.example.incidentreporter.entity.Incident;
import com.example.incidentreporter.entity.IncidentConfirmation;
import com.example.incidentreporter.entity.User;
import com.example.incidentreporter.entity.UserLocation;
import com.example.incidentreporter.enums.ConfirmationAction;
import com.example.incidentreporter.enums.IncidentStatus;
import com.example.incidentreporter.exception.EntityNotFoundException;
import com.example.incidentreporter.repository.IncidentConfirmationRepository;
import com.example.incidentreporter.repository.IncidentRepository;
import com.example.incidentreporter.repository.UserLocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentConfirmationRepository incidentConfirmationRepository;
    private final UserLocationRepository userLocationRepository;
    private final FCMService fcmService;
    private final NotificationService notificationService;
    private final UserService userService;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    /**
     * Crea un nuevo incidente y notifica a usuarios cercanos
     */
    @Transactional
    public IncidentDTO createIncident(IncidentRequest request, User currentUser) {
        // Crear el incidente
        Incident incident = Incident.builder()
                .reporter(currentUser)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .location(geometryFactory.createPoint(new Coordinate(request.getLongitude(), request.getLatitude())))
                .address(request.getAddress())
                .title(request.getTitle())
                .description(request.getDescription())
                .severity(request.getSeverity())
                .category(request.getCategory())
                .status(IncidentStatus.ACTIVE)
                .radius(request.getRadius())
                .imageUrls(request.getImageUrls())
                .audioUrl(request.getAudioUrl())
                .build();

        incident = incidentRepository.save(incident);

        // Actualizar estadísticas del usuario
        currentUser.setTotalIncidentsReported(currentUser.getTotalIncidentsReported() + 1);
        userService.saveUser(currentUser);

        // Notificar a usuarios cercanos
        notifyNearbyUsers(incident);

        return mapToDTO(incident);
    }

    /**
     * Obtiene incidentes cercanos a una ubicación
     */
    @Transactional(readOnly = true)
    public List<IncidentDTO> getIncidentsNearby(double latitude, double longitude, double radius, User currentUser) {
        Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        List<Object[]> results = incidentRepository.findIncidentsWithinRadius(point, radius);

        return results.stream().map(result -> {
            Incident incident = (Incident) result[0];
            Double distance = (Double) result[1];

            IncidentDTO dto = mapToDTO(incident);
            dto.setDistance(distance);

            // Verificar si el usuario ya ha confirmado este incidente
            boolean hasConfirmed = hasUserConfirmedIncident(incident, currentUser);
            dto.setUserHasConfirmed(hasConfirmed);

            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Obtiene incidentes cercanos paginados
     */
    @Transactional(readOnly = true)
    public Page<IncidentDTO> getIncidentsNearbyPaged(double latitude, double longitude, double radius, User currentUser, Pageable pageable) {
        Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        Page<Object[]> results = incidentRepository.findIncidentsWithinRadiusPaged(point, radius, pageable);

        return results.map(result -> {
            Incident incident = (Incident) result[0];
            Double distance = (Double) result[1];

            IncidentDTO dto = mapToDTO(incident);
            dto.setDistance(distance);

            // Verificar si el usuario ya ha confirmado este incidente
            boolean hasConfirmed = hasUserConfirmedIncident(incident, currentUser);
            dto.setUserHasConfirmed(hasConfirmed);

            return dto;
        });
    }

    /**
     * Obtiene un incidente por su ID
     */
    @Transactional(readOnly = true)
    public IncidentDTO getIncidentById(String id, User currentUser) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Incidente no encontrado con ID: " + id));

        IncidentDTO dto = mapToDTO(incident);

        // Verificar si el usuario ya ha confirmado este incidente
        boolean hasConfirmed = hasUserConfirmedIncident(incident, currentUser);
        dto.setUserHasConfirmed(hasConfirmed);

        // Si tenemos la ubicación actual del usuario, calcular distancia
        Optional<UserLocation> userLocation = userLocationRepository.findTopByUserOrderByTimestampDesc(currentUser);
        userLocation.ifPresent(location -> {
            double distance = calculateDistance(
                    location.getLatitude(), location.getLongitude(),
                    incident.getLatitude(), incident.getLongitude()
            );
            dto.setDistance(distance);
        });

        return dto;
    }

    /**
     * Confirma o niega un incidente
     */
    @Transactional
    public IncidentDTO confirmIncident(String id, IncidentConfirmationRequest request, User currentUser) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Incidente no encontrado con ID: " + id));

        // Verificar si el usuario ya ha confirmado este incidente
        Optional<IncidentConfirmation> existingConfirmation =
                incidentConfirmationRepository.findByIncidentAndUser(incident, currentUser);

        IncidentConfirmation confirmation;

        if (existingConfirmation.isPresent()) {
            // Actualizar confirmación existente
            confirmation = existingConfirmation.get();

            // Si la acción anterior era CONFIRMED y ahora es DENIED, ajustar contadores
            if (confirmation.getAction() == ConfirmationAction.CONFIRMED &&
                    request.getAction() == ConfirmationAction.DENIED) {
                incident.setConfirmationCount(incident.getConfirmationCount() - 1);
                incident.setDenialCount(incident.getDenialCount() + 1);
            }
            // Si la acción anterior era DENIED y ahora es CONFIRMED, ajustar contadores
            else if (confirmation.getAction() == ConfirmationAction.DENIED &&
                    request.getAction() == ConfirmationAction.CONFIRMED) {
                incident.setConfirmationCount(incident.getConfirmationCount() + 1);
                incident.setDenialCount(incident.getDenialCount() - 1);
            }

            confirmation.setAction(request.getAction());
            confirmation.setTimestamp(LocalDateTime.now());
            confirmation.setUserLatitude(request.getUserLatitude());
            confirmation.setUserLongitude(request.getUserLongitude());
            confirmation.setComment(request.getComment());
            confirmation.setConfidence(request.getConfidence());
            confirmation.setNotificationDelay(request.getNotificationDelay());
            confirmation.setDeviceType(request.getDeviceType());
        } else {
            // Crear nueva confirmación
            confirmation = IncidentConfirmation.builder()
                    .incident(incident)
                    .user(currentUser)
                    .action(request.getAction())
                    .userLatitude(request.getUserLatitude())
                    .userLongitude(request.getUserLongitude())
                    .comment(request.getComment())
                    .confidence(request.getConfidence())
                    .notificationDelay(request.getNotificationDelay())
                    .deviceType(request.getDeviceType())
                    .build();

            // Actualizar contadores
            if (request.getAction() == ConfirmationAction.CONFIRMED) {
                incident.setConfirmationCount(incident.getConfirmationCount() + 1);
                // Actualizar estadísticas del usuario
                currentUser.setTotalConfirmations(currentUser.getTotalConfirmations() + 1);
                userService.saveUser(currentUser);
            } else if (request.getAction() == ConfirmationAction.DENIED) {
                incident.setDenialCount(incident.getDenialCount() + 1);
            }
        }

        // Si acción es CONFIRMED, actualizar lastConfirmationAt
        if (request.getAction() == ConfirmationAction.CONFIRMED) {
            incident.setLastConfirmationAt(LocalDateTime.now());
        }

        // Calcular nueva intensidad
        updateIncidentIntensity(incident);

        // Guardar confirmación y actualizar incidente
        incidentConfirmationRepository.save(confirmation);
        incident = incidentRepository.save(incident);

        // Si hay muchas confirmaciones, considerar actualizaciones
        if (incident.getConfirmationCount() >= 5 && incident.getDenialCount() <= incident.getConfirmationCount() / 3) {
            // Notificar mediante FCM si hay cambios significativos de intensidad
            notifySignificantIntensityChanges(incident);
        }

        // Si hay muchas negaciones, considerar marcar como disputado
        if (incident.getDenialCount() >= 10 && incident.getDenialCount() > incident.getConfirmationCount() * 2) {
            incident.setStatus(IncidentStatus.DISPUTED);
            incidentRepository.save(incident);
        }

        return mapToDTO(incident);
    }

    /**
     * Actualiza la intensidad del incidente basada en confirmaciones
     */
    public void updateIncidentIntensity(Incident incident) {
        final int DECAY_TIME_HOURS = 2; // Tiempo fijo después del cual vuelve al color normal

        if (incident.getLastConfirmationAt() == null) {
            incident.setIntensityLevel(0); // Sin confirmaciones = color normal
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        double hoursElapsed = java.time.Duration.between(incident.getLastConfirmationAt(), now).toMillis() / (1000.0 * 60 * 60);

        if (hoursElapsed >= DECAY_TIME_HOURS) {
            incident.setIntensityLevel(0); // Vuelve al color normal después del tiempo fijo
            return;
        }

        // Intensidad basada en confirmaciones (max 100)
        double confirmationIntensity = Math.min(100, (incident.getConfirmationCount() / 5.0) * 100);

        // Factor de decay lineal durante las 2 horas
        double decayFactor = Math.max(0, 1 - (hoursElapsed / DECAY_TIME_HOURS));

        incident.setIntensityLevel(confirmationIntensity * decayFactor);
    }

    /**
     * Job programado para actualizar intensidades de incidentes
     */
    @Scheduled(fixedRate = 300000) // Cada 5 minutos
    @Transactional
    public void updateAllIncidentIntensities() {
        List<Incident> activeIncidents = incidentRepository.findByStatusOrderByCreatedAtDesc(IncidentStatus.ACTIVE);

        for (Incident incident : activeIncidents) {
            double oldIntensity = incident.getIntensityLevel();
            updateIncidentIntensity(incident);

            // Si hay un cambio significativo en la intensidad, guardar y posiblemente notificar
            if (Math.abs(oldIntensity - incident.getIntensityLevel()) > 20) {
                incidentRepository.save(incident);

                // Notificar a usuarios cercanos sobre cambios significativos
                if (incident.getIntensityLevel() >= 70 && oldIntensity < 70) {
                    notifySignificantIntensityChanges(incident);
                }
            }
        }
    }

    /**
     * Job programado para marcar incidentes expirados
     */
    @Scheduled(fixedRate = 3600000) // Cada 1 hora
    @Transactional
    public void markExpiredIncidents() {
        LocalDateTime now = LocalDateTime.now();
        List<Incident> expiredIncidents = incidentRepository.findExpiredIncidents(now);

        for (Incident incident : expiredIncidents) {
            incident.setStatus(IncidentStatus.EXPIRED);
            incidentRepository.save(incident);
            log.info("Incident marked as expired: {}", incident.getId());
        }
    }

    /**
     * Envía notificaciones a usuarios cercanos sobre un nuevo incidente
     */
    private void notifyNearbyUsers(Incident incident) {
        Point point = incident.getLocation();
        List<User> nearbyUsers = userLocationRepository.findDistinctUsersWithinRadius(point, incident.getRadius());

        List<String> validTokens = new ArrayList<>();

        for (User user : nearbyUsers) {
            // No notificar al creador del incidente
            if (user.getId().equals(incident.getReporter().getId())) {
                continue;
            }

            // Solo notificar a usuarios con notificaciones habilitadas
            if (!user.isNotificationsEnabled()) {
                continue;
            }

            // Solo notificar a usuarios con tokens FCM válidos
            if (user.getFcmToken() == null || user.getFcmToken().isEmpty()) {
                continue;
            }

            validTokens.add(user.getFcmToken());

            // Crear notificación en el sistema
            notificationService.createIncidentNotification(user, incident);
        }

        // Enviar notificaciones FCM en lote si hay tokens válidos
        if (!validTokens.isEmpty()) {
            fcmService.sendToMultipleUsers(validTokens, incident);

            // Actualizar contador de notificaciones
            incident.setTotalNotifications(incident.getTotalNotifications() + validTokens.size());
            incidentRepository.save(incident);
        }
    }

    /**
     * Notifica a usuarios cercanos sobre cambios significativos en la intensidad
     */
    private void notifySignificantIntensityChanges(Incident incident) {
        Point point = incident.getLocation();
        List<User> nearbyUsers = userLocationRepository.findDistinctUsersWithinRadius(point, incident.getRadius());

        List<String> validTokens = new ArrayList<>();

        for (User user : nearbyUsers) {
            // No notificar al creador del incidente
            if (user.getId().equals(incident.getReporter().getId())) {
                continue;
            }

            // Solo notificar a usuarios con notificaciones habilitadas
            if (!user.isNotificationsEnabled()) {
                continue;
            }

            // Solo notificar a usuarios con tokens FCM válidos
            if (user.getFcmToken() == null || user.getFcmToken().isEmpty()) {
                continue;
            }

            // Verificar si ya hay una confirmación del usuario
            boolean hasConfirmed = hasUserConfirmedIncident(incident, user);
            if (hasConfirmed) {
                continue; // No notificar a usuarios que ya han interactuado
            }

            validTokens.add(user.getFcmToken());

            // Crear notificación de actualización en el sistema
            notificationService.createIncidentUpdateNotification(user, incident);
        }

        // Enviar notificaciones FCM en lote si hay tokens válidos
        if (!validTokens.isEmpty()) {
            fcmService.sendToMultipleUsers(validTokens, incident);
        }
    }

    /**
     * Verifica si un usuario ya ha confirmado un incidente
     */
    private boolean hasUserConfirmedIncident(Incident incident, User user) {
        return incidentConfirmationRepository.findByIncidentAndUser(incident, user).isPresent();
    }

    /**
     * Convierte una entidad Incident a un DTO
     */
    private IncidentDTO mapToDTO(Incident incident) {
        return IncidentDTO.builder()
                .id(incident.getId())
                .reporterId(incident.getReporter().getId())
                .reporterEmail(incident.getReporter().getEmail())
                .latitude(incident.getLatitude())
                .longitude(incident.getLongitude())
                .address(incident.getAddress())
                .title(incident.getTitle())
                .description(incident.getDescription())
                .severity(incident.getSeverity())
                .category(incident.getCategory())
                .status(incident.getStatus())
                .createdAt(incident.getCreatedAt())
                .updatedAt(incident.getUpdatedAt())
                .expiresAt(incident.getExpiresAt())
                .confirmationCount(incident.getConfirmationCount())
                .denialCount(incident.getDenialCount())
                .radius(incident.getRadius())
                .intensityLevel(incident.getIntensityLevel())
                .lastConfirmationAt(incident.getLastConfirmationAt())
                .imageUrls(incident.getImageUrls())
                .audioUrl(incident.getAudioUrl())
                .build();
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