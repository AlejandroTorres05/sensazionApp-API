package com.example.incidentreporter.repository;

import com.example.incidentreporter.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByAuth0Id(String auth0Id);
    Optional<User> findByEmail(String email);
    Optional<User> findByFcmToken(String fcmToken);
}