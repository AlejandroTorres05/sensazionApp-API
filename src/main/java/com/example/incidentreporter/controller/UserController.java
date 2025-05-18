package com.example.incidentreporter.controller;

import com.example.incidentreporter.dto.UserDTO;
import com.example.incidentreporter.entity.User;
import com.example.incidentreporter.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Obtener perfil del usuario
     */
    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getUserProfile(Authentication authentication) {
        User user = userService.getUserByAuth0Id(authentication.getName());
        UserDTO userDTO = UserDTO.builder()
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

        return ResponseEntity.ok(userDTO);
    }

    /**
     * Actualizar perfil del usuario
     */
    @PutMapping("/profile")
    public ResponseEntity<UserDTO> updateProfile(
            @RequestBody UserDTO userDTO,
            Authentication authentication) {
        User user = userService.getUserByAuth0Id(authentication.getName());

        // Actualizar campos permitidos
        if (userDTO.getFirstName() != null) user.setFirstName(userDTO.getFirstName());
        if (userDTO.getLastName() != null) user.setLastName(userDTO.getLastName());
        if (userDTO.getPhone() != null) user.setPhone(userDTO.getPhone());
        if (userDTO.getAvatarUrl() != null) user.setAvatarUrl(userDTO.getAvatarUrl());

        user.setProfileCompleted(true);
        User savedUser = userService.saveUser(user);

        UserDTO updatedUserDTO = UserDTO.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .phone(savedUser.getPhone())
                .avatarUrl(savedUser.getAvatarUrl())
                .notificationsEnabled(savedUser.isNotificationsEnabled())
                .locationSharingEnabled(savedUser.isLocationSharingEnabled())
                .notificationRadius(savedUser.getNotificationRadius())
                .totalIncidentsReported(savedUser.getTotalIncidentsReported())
                .totalConfirmations(savedUser.getTotalConfirmations())
                .verificationScore(savedUser.getVerificationScore())
                .createdAt(savedUser.getCreatedAt())
                .lastActiveAt(savedUser.getLastActiveAt())
                .build();

        return ResponseEntity.ok(updatedUserDTO);
    }

    /**
     * Actualizar configuraciones del usuario
     */
    @PutMapping("/settings")
    public ResponseEntity<UserDTO> updateSettings(
            @RequestBody Map<String, Object> settings,
            Authentication authentication) {
        User user = userService.getUserByAuth0Id(authentication.getName());

        boolean notificationsEnabled = settings.containsKey("notificationsEnabled") ?
                (boolean) settings.get("notificationsEnabled") : user.isNotificationsEnabled();

        boolean locationSharingEnabled = settings.containsKey("locationSharingEnabled") ?
                (boolean) settings.get("locationSharingEnabled") : user.isLocationSharingEnabled();

        double notificationRadius = settings.containsKey("notificationRadius") ?
                Double.parseDouble(settings.get("notificationRadius").toString()) : user.getNotificationRadius();

        UserDTO updatedUser = userService.updateUserSettings(
                user, notificationsEnabled, locationSharingEnabled, notificationRadius);

        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Actualizar token FCM (este endpoint permite acceso sin autenticación)
     */
    @PutMapping("/fcm-token")
    public ResponseEntity<Void> updateFCMToken(@RequestBody Map<String, String> tokenRequest) {
        String auth0Id = tokenRequest.get("auth0Id");
        String fcmToken = tokenRequest.get("fcmToken");

        if (auth0Id == null || fcmToken == null) {
            return ResponseEntity.badRequest().build();
        }

        User user = userService.updateFCMToken(auth0Id, fcmToken);

        if (user == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok().build();
    }
}