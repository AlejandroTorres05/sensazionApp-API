package com.example.incidentreporter.controller;

import com.example.incidentreporter.dto.LocationDTO;
import com.example.incidentreporter.dto.UserLocationRequest;
import com.example.incidentreporter.entity.User;
import com.example.incidentreporter.service.LocationService;
import com.example.incidentreporter.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;
    private final UserService userService;

    /**
     * Actualizar ubicación de usuario
     */
    @PostMapping
    public ResponseEntity<LocationDTO> updateLocation(
            @Valid @RequestBody UserLocationRequest request,
            Authentication authentication) {
        User currentUser = userService.getUserByAuth0Id(authentication.getName());
        LocationDTO location = locationService.updateUserLocation(request, currentUser);

        if (location == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(location);
    }

    /**
     * Obtener ubicación actual del usuario
     */
    @GetMapping("/current")
    public ResponseEntity<LocationDTO> getCurrentLocation(Authentication authentication) {
        User currentUser = userService.getUserByAuth0Id(authentication.getName());
        LocationDTO location = locationService.getCurrentLocation(currentUser);

        if (location == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(location);
    }
}