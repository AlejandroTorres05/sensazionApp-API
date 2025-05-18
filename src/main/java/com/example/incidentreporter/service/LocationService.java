package com.example.incidentreporter.service;

import com.example.incidentreporter.dto.LocationDTO;
import com.example.incidentreporter.dto.UserLocationRequest;
import com.example.incidentreporter.entity.User;
import com.example.incidentreporter.entity.UserLocation;
import com.example.incidentreporter.repository.UserLocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocationService {

    private final UserLocationRepository userLocationRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    /**
     * Actualiza la ubicación del usuario
     */
    @Transactional
    public LocationDTO updateUserLocation(UserLocationRequest request, User currentUser) {
        // Verificar si el usuario tiene habilitado el compartir ubicación
        if (!currentUser.isLocationSharingEnabled()) {
            log.warn("User {} attempted to update location but location sharing is disabled", currentUser.getEmail());
            return null;
        }

        // Desactivar ubicaciones anteriores
        Optional<UserLocation> activeLocation = userLocationRepository.findActiveLocationByUser(currentUser);
        activeLocation.ifPresent(location -> {
            location.setActive(false);
            userLocationRepository.save(location);
        });

        // Crear nuevo punto de ubicación
        Point point = geometryFactory.createPoint(new Coordinate(request.getLongitude(), request.getLatitude()));

        // Crear y guardar nueva ubicación
        UserLocation userLocation = UserLocation.builder()
                .user(currentUser)
                .location(point)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .accuracy(request.getAccuracy())
                .timestamp(LocalDateTime.now())
                .isActive(true)
                .build();

        userLocation = userLocationRepository.save(userLocation);

        // Actualizar última actividad del usuario
        currentUser.setLastActiveAt(LocalDateTime.now());

        return mapToDTO(userLocation);
    }

    /**
     * Obtiene la ubicación actual del usuario
     */
    @Transactional(readOnly = true)
    public LocationDTO getCurrentLocation(User currentUser) {
        Optional<UserLocation> activeLocation = userLocationRepository.findActiveLocationByUser(currentUser);
        return activeLocation.map(this::mapToDTO).orElse(null);
    }

    /**
     * Limpia ubicaciones antiguas (más de 1 hora)
     */
    @Scheduled(fixedRate = 3600000) // Cada 1 hora
    @Transactional
    public void cleanOldLocations() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<UserLocation> oldLocations = userLocationRepository.findLocationsOlderThan(oneHourAgo);

        log.info("Cleaning {} old locations", oldLocations.size());
        userLocationRepository.deleteAll(oldLocations);
    }

    /**
     * Convierte una entidad UserLocation a un DTO
     */
    private LocationDTO mapToDTO(UserLocation userLocation) {
        return LocationDTO.builder()
                .id(userLocation.getId())
                .userId(userLocation.getUser().getId())
                .latitude(userLocation.getLatitude())
                .longitude(userLocation.getLongitude())
                .accuracy(userLocation.getAccuracy())
                .timestamp(userLocation.getTimestamp())
                .isActive(userLocation.isActive())
                .build();
    }
}