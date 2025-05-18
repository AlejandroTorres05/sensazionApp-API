package com.example.incidentreporter.repository;

import com.example.incidentreporter.entity.Incident;
import com.example.incidentreporter.entity.User;
import com.example.incidentreporter.enums.IncidentStatus;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, String> {

    @Query(value = "SELECT i, ST_DistanceSphere(i.location, :point) as distance " +
            "FROM Incident i " +
            "WHERE ST_DistanceSphere(i.location, :point) <= :radius " +
            "AND i.status = 'ACTIVE' " +
            "ORDER BY distance ASC")
    List<Object[]> findIncidentsWithinRadius(@Param("point") Point point, @Param("radius") double radius);

    @Query(value = "SELECT i, ST_DistanceSphere(i.location, :point) as distance " +
            "FROM Incident i " +
            "WHERE ST_DistanceSphere(i.location, :point) <= :radius " +
            "AND i.status = 'ACTIVE' " +
            "ORDER BY distance ASC")
    Page<Object[]> findIncidentsWithinRadiusPaged(@Param("point") Point point, @Param("radius") double radius, Pageable pageable);

    @Query(value = "SELECT i FROM Incident i WHERE i.status = 'ACTIVE' AND i.expiresAt < :now")
    List<Incident> findExpiredIncidents(@Param("now") LocalDateTime now);

    List<Incident> findByReporterAndStatusOrderByCreatedAtDesc(User reporter, IncidentStatus status);

    List<Incident> findByStatusOrderByCreatedAtDesc(IncidentStatus status);

    @Query(value = "SELECT i FROM Incident i WHERE i.status = 'ACTIVE' ORDER BY i.createdAt DESC")
    Page<Incident> findActiveIncidents(Pageable pageable);
}