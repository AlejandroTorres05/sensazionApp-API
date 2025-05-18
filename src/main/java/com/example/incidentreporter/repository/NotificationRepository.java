package com.example.incidentreporter.repository;

import com.example.incidentreporter.entity.Incident;
import com.example.incidentreporter.entity.Notification;
import com.example.incidentreporter.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    List<Notification> findByUserAndIncidentAndStatus(User user, Incident incident, Notification.NotificationStatus status);

    List<Notification> findByStatusAndPushNotificationSent(Notification.NotificationStatus status, boolean pushNotificationSent);
}