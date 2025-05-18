package com.example.incidentreporter.controller;

import com.example.incidentreporter.dto.IncidentConfirmationRequest;
import com.example.incidentreporter.dto.IncidentDTO;
import com.example.incidentreporter.dto.IncidentRequest;
import com.example.incidentreporter.entity.User;
import com.example.incidentreporter.service.IncidentService;
import com.example.incidentreporter.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;
    private final UserService userService;

    /**
     * Crear nuevo incidente
     */
    @PostMapping
    public ResponseEntity<IncidentDTO> createIncident(
            @Valid @RequestBody IncidentRequest request,
            Authentication authentication) {
        User currentUser = userService.getUserByAuth0Id(authentication.getName());
        IncidentDTO createdIncident = incidentService.createIncident(request, currentUser);
        return ResponseEntity.ok(createdIncident);
    }

    /**
     * Obtener incidentes cercanos
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<IncidentDTO>> getIncidentsNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "1000") double radius,
            Authentication authentication) {
        User currentUser = userService.getUserByAuth0Id(authentication.getName());
        List<IncidentDTO> incidents = incidentService.getIncidentsNearby(lat, lng, radius, currentUser);
        return ResponseEntity.ok(incidents);
    }

    /**
     * Obtener incidentes cercanos con paginación
     */
    @GetMapping("/nearby/paged")
    public ResponseEntity<Page<IncidentDTO>> getIncidentsNearbyPaged(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "1000") double radius,
            Pageable pageable,
            Authentication authentication) {
        User currentUser = userService.getUserByAuth0Id(authentication.getName());
        Page<IncidentDTO> incidents = incidentService.getIncidentsNearbyPaged(lat, lng, radius, currentUser, pageable);
        return ResponseEntity.ok(incidents);
    }

    /**
     * Obtener incidente por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<IncidentDTO> getIncidentById(
            @PathVariable String id,
            Authentication authentication) {
        User currentUser = userService.getUserByAuth0Id(authentication.getName());
        IncidentDTO incident = incidentService.getIncidentById(id, currentUser);
        return ResponseEntity.ok(incident);
    }

    /**
     * Confirmar o negar incidente
     */
    @PutMapping("/{id}/confirm")
    public ResponseEntity<IncidentDTO> confirmIncident(
            @PathVariable String id,
            @Valid @RequestBody IncidentConfirmationRequest request,
            Authentication authentication) {
        User currentUser = userService.getUserByAuth0Id(authentication.getName());
        IncidentDTO incident = incidentService.confirmIncident(id, request, currentUser);
        return ResponseEntity.ok(incident);
    }
}