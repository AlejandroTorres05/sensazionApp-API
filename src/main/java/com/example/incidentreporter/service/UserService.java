package com.example.incidentreporter.service;

import com.example.incidentreporter.dto.UserDTO;
import com.example.incidentreporter.entity.User;
import com.example.incidentreporter.exception.EntityNotFoundException;
import com.example.incidentreporter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    // SOLUCIÓN: Inyección lazy para evitar dependencia circular
    @Lazy
    private final FCMService fcmService;

    /**
     * Obtiene un usuario por su ID
     */
    @Transactional(readOnly = true)
    public User getUserById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + id));
    }

    /**
     * Obtiene un usuario por su Auth0ID
     */
    @Transactional(readOnly = true)
    public User getUserByAuth0Id(String auth0Id) {
        return userRepository.findByAuth0Id(auth0Id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con Auth0ID: " + auth0Id));
    }

    /**
     * Obtiene o crea un usuario basado en información de Auth0
     */
    @Transactional
    public User getOrCreateUser(String auth0Id, String email) {
        Optional<User> existingUser = userRepository.findByAuth0Id(auth0Id);

        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        // Crear nuevo usuario
        User newUser = User.builder()
                .auth0Id(auth0Id)
                .email(email)
                .build();

        return userRepository.save(newUser);
    }

    /**
     * Guarda un usuario
     */
    @Transactional
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    /**
     * Actualiza el token FCM de un usuario
     * VERSIÓN SIMPLIFICADA: Solo guarda el token, la validación se hace en el controlador
     */
    @Transactional
    public User updateFCMToken(String auth0Id, String fcmToken) {
        // Buscar usuario por auth0Id
        User user = userRepository.findByAuth0Id(auth0Id)
                .orElse(null);

        if (user == null) {
            log.warn("User not found with Auth0ID: {}", auth0Id);
            return null;
        }

        // Actualizar token (la validación la haremos en el controlador si es necesario)
        user.setFcmToken(fcmToken);
        return userRepository.save(user);
    }

    /**
     * Actualiza la configuración de notificaciones del usuario
     */
    @Transactional
    public UserDTO updateUserSettings(User user, boolean notificationsEnabled,
                                      boolean locationSharingEnabled, double notificationRadius) {
        user.setNotificationsEnabled(notificationsEnabled);
        user.setLocationSharingEnabled(locationSharingEnabled);
        user.setNotificationRadius(notificationRadius);

        User savedUser = userRepository.save(user);
        return mapToDTO(savedUser);
    }

    /**
     * Encuentra usuarios con token FCM
     */
    @Transactional(readOnly = true)
    public List<User> findUsersWithFCMToken() {
        return userRepository.findAll().stream()
                .filter(user -> user.getFcmToken() != null && !user.getFcmToken().isEmpty())
                .toList();
    }

    /**
     * Encuentra usuario por token FCM
     */
    @Transactional(readOnly = true)
    public Optional<User> findByFcmToken(String fcmToken) {
        return userRepository.findByFcmToken(fcmToken);
    }

    /**
     * Valida un token FCM usando el servicio FCM
     * Método público para uso externo cuando sea necesario
     */
    public boolean validateFCMToken(String fcmToken) {
        try {
            return fcmService.validateFCMToken(fcmToken);
        } catch (Exception e) {
            log.error("Error validating FCM token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Convierte una entidad User a un DTO
     */
    private UserDTO mapToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .notificationsEnabled(user.isNotificationsEnabled())
                .locationSharingEnabled(user.isLocationSharingEnabled())
                .notificationRadius(user.getNotificationRadius())
                .totalIncidentsReported(user.getTotalIncidentsReported())
                .totalConfirmations(user.getTotalConfirmations())
                .verificationScore(user.getVerificationScore())
                .createdAt(user.getCreatedAt())
                .lastActiveAt(user.getLastActiveAt())
                .build();
    }
}