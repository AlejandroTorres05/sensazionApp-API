package com.example.incidentreporter.controller;

import com.example.incidentreporter.entity.Notification;
import com.example.incidentreporter.entity.User;
import com.example.incidentreporter.service.NotificationService;
import com.example.incidentreporter.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    /**
     * Obtener notificaciones del usuario
     */
    @GetMapping
    public ResponseEntity<Page<Notification>> getUserNotifications(
            Pageable pageable,
            Authentication authentication) {
        User currentUser = userService.getUserByAuth0Id(authentication.getName());
        Page<Notification> notifications = notificationService.getUserNotifications(currentUser, pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Marcar notificación como leída
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(
            @PathVariable String id,
            Authentication authentication) {
        User currentUser = userService.getUserByAuth0Id(authentication.getName());
        Notification notification = notificationService.markAsRead(id, currentUser);
        return ResponseEntity.ok(notification);
    }
}