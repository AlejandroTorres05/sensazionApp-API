package com.example.incidentreporter.repository;

import com.example.incidentreporter.entity.User;
import com.example.incidentreporter.entity.UserLocation;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserLocationRepository extends JpaRepository<UserLocation, String> {

    Optional<UserLocation> findTopByUserOrderByTimestampDesc(User user);

    @Query(value = "SELECT ul FROM UserLocation ul WHERE ul.user = :user AND ul.isActive = true")
    Optional<UserLocation> findActiveLocationByUser(@Param("user") User user);

    @Query(value = "SELECT ul FROM UserLocation ul WHERE ST_DistanceSphere(ul.location, :point) <= :radius AND ul.isActive = true")
    List<UserLocation> findUsersWithinRadius(@Param("point") Point point, @Param("radius") double radius);

    @Query(value = "SELECT DISTINCT ul.user FROM UserLocation ul WHERE ST_DistanceSphere(ul.location, :point) <= :radius AND ul.isActive = true")
    List<User> findDistinctUsersWithinRadius(@Param("point") Point point, @Param("radius") double radius);

    @Query(value = "SELECT ul FROM UserLocation ul WHERE ul.timestamp < :timestamp")
    List<UserLocation> findLocationsOlderThan(@Param("timestamp") LocalDateTime timestamp);
}