package com.example.incidentreporter.controller;

import com.example.incidentreporter.dto.UserDTO;
import com.example.incidentreporter.entity.User;
import com.example.incidentreporter.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
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
                .profileCompleted(user.isProfileCompleted())
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
     * Registra un nuevo usuario después de la autenticación con Auth0.
     * Crea el registro básico con valores por defecto y perfil marcado como incompleto.
     *
     * @param authentication Datos del usuario autenticado (auth0Id, email)
     * @return UserDTO con información básica y profileCompleted = false
     */
    @PostMapping("/register")
    public ResponseEntity<UserDTO> registerUser(
            Authentication authentication,
            HttpServletRequest request
    ) {
        String auth0Id = authentication.getName();
        String email = null;

        // Extraer el token del header Authorization
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                // Llamar al endpoint /userinfo de Auth0 para obtener el email
                String userInfoUrl = "https://dev-vguxq7gicpeheej8.us.auth0.com/userinfo";

                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(token);

                HttpEntity<String> entity = new HttpEntity<>(headers);
                ResponseEntity<Map> response = restTemplate.exchange(
                        userInfoUrl,
                        HttpMethod.GET,
                        entity,
                        Map.class
                );

                if (response.getStatusCode().is2xxSuccessful()) {
                    Map<String, Object> userInfo = response.getBody();
                    System.out.println("UserInfo response: " + userInfo);

                    if (userInfo != null && userInfo.containsKey("email")) {
                        email = (String) userInfo.get("email");
                        System.out.println("Email from userinfo: " + email);
                    }
                }

            } catch (Exception e) {
                System.out.println("Error calling userinfo: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (email == null) {
            System.out.println("ERROR: Email is null!");
            return ResponseEntity.badRequest().build();
        }

        User user = User.builder()
                .auth0Id(auth0Id)
                .email(email)
                .profileCompleted(false)
                .notificationsEnabled(true)
                .locationSharingEnabled(true)
                .notificationRadius(1000.0) // 1km por defecto
                .totalIncidentsReported(0)
                .totalConfirmations(0)
                .verificationScore(0.0)
                .build();

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
                .profileCompleted(user.isProfileCompleted())
                .createdAt(savedUser.getCreatedAt())
                .lastActiveAt(savedUser.getLastActiveAt())
                .build();

        return ResponseEntity.ok(updatedUserDTO);
    }

    /**
     * Completa el perfil del usuario con información personal obligatoria.
     * Campos requeridos: firstName, lastName, phone, notificationRadius > 0
     * Campo opcional: avatarUrl
     *
     * @param userDTO Datos del perfil a completar
     * @param authentication Usuario autenticado
     * @return UserDTO actualizado con profileCompleted = true
     */
    @PutMapping("/complete-profile")
    public ResponseEntity<UserDTO> completeProfile(
            @RequestBody UserDTO userDTO,
            Authentication authentication) {

        User user = userService.getUserByAuth0Id(authentication.getName());

        // Validar campos obligatorios
        if (userDTO.getFirstName() == null || userDTO.getFirstName().trim().isEmpty() ||
                userDTO.getLastName() == null || userDTO.getLastName().trim().isEmpty() ||
                userDTO.getPhone() == null || userDTO.getPhone().trim().isEmpty() ||
                userDTO.getNotificationRadius() <= 0) {
            return ResponseEntity.badRequest().build();
        }

        user.setFirstName(userDTO.getFirstName().trim());
        user.setLastName(userDTO.getLastName().trim());
        user.setPhone(userDTO.getPhone().trim());
        user.setNotificationRadius(userDTO.getNotificationRadius());

        // Avatar opcional
        if (userDTO.getAvatarUrl() != null) {
            user.setAvatarUrl(userDTO.getAvatarUrl());
        }

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
                .profileCompleted(user.isProfileCompleted())
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