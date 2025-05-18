package com.example.incidentreporter.controller;

import com.example.incidentreporter.entity.User;
import com.example.incidentreporter.security.JwtTokenProvider;
import com.example.incidentreporter.security.UserPrincipal;
import com.example.incidentreporter.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider tokenProvider;

    /**
     * Endpoint para registro/login con Auth0
     * Cuando un usuario se autentica con Auth0 en el frontend, envía su auth0Id y email
     * a este endpoint para obtener un JWT
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {
        String auth0Id = loginRequest.get("auth0Id");
        String email = loginRequest.get("email");

        if (auth0Id == null || email == null) {
            return ResponseEntity.badRequest().build();
        }

        // Obtener o crear usuario basado en Auth0
        User user = userService.getOrCreateUser(auth0Id, email);

        // Generar token JWT
        UserPrincipal userPrincipal = new UserPrincipal(auth0Id);
        String jwt = tokenProvider.generateToken(userPrincipal);

        // Respuesta con token y datos básicos de usuario
        Map<String, Object> response = Map.of(
                "token", jwt,
                "user", Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "profileCompleted", user.isProfileCompleted()
                )
        );

        return ResponseEntity.ok(response);
    }
}