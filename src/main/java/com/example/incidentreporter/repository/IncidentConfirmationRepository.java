package com.example.incidentreporter.repository;

import com.example.incidentreporter.entity.Incident;
import com.example.incidentreporter.entity.IncidentConfirmation;
import com.example.incidentreporter.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentConfirmationRepository extends JpaRepository<IncidentConfirmation, String> {

    Optional<IncidentConfirmation> findByIncidentAndUser(Incident incident, User user);

    List<IncidentConfirmation> findByIncident(Incident incident);

    @Query(value = "SELECT COUNT(ic) FROM IncidentConfirmation ic WHERE ic.incident = :incident AND ic.action = 'CONFIRMED'")
    long countConfirmationsByIncident(@Param("incident") Incident incident);

    @Query(value = "SELECT COUNT(ic) FROM IncidentConfirmation ic WHERE ic.incident = :incident AND ic.action = 'DENIED'")
    long countDenialsByIncident(@Param("incident") Incident incident);
}