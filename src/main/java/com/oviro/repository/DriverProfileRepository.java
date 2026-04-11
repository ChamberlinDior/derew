package com.oviro.repository;

import com.oviro.enums.DriverStatus;
import com.oviro.model.DriverProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DriverProfileRepository extends JpaRepository<DriverProfile, UUID> {

    Optional<DriverProfile> findByUserId(UUID userId);

    Optional<DriverProfile> findByLicenseNumber(String licenseNumber);

    Page<DriverProfile> findByStatus(DriverStatus status, Pageable pageable);

    Page<DriverProfile> findByPartnerId(UUID partnerId, Pageable pageable);

    @Query("""
        SELECT d FROM DriverProfile d
        WHERE d.status = 'ONLINE'
        AND (6371 * acos(cos(radians(:lat)) * cos(radians(d.currentLatitude))
            * cos(radians(d.currentLongitude) - radians(:lng))
            + sin(radians(:lat)) * sin(radians(d.currentLatitude)))) <= :radiusKm
        ORDER BY (6371 * acos(cos(radians(:lat)) * cos(radians(d.currentLatitude))
            * cos(radians(d.currentLongitude) - radians(:lng))
            + sin(radians(:lat)) * sin(radians(d.currentLatitude)))) ASC
        """)
    List<DriverProfile> findNearbyOnlineDrivers(
            @Param("lat") BigDecimal lat,
            @Param("lng") BigDecimal lng,
            @Param("radiusKm") double radiusKm
    );

    long countByStatus(DriverStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE DriverProfile d
        SET d.currentLatitude = :latitude,
            d.currentLongitude = :longitude
        WHERE d.user.id = :userId
    """)
    int updateCurrentLocationByUserId(
            @Param("userId") UUID userId,
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE DriverProfile d
        SET d.status = :status
        WHERE d.user.id = :userId
    """)
    int updateStatusByUserId(
            @Param("userId") UUID userId,
            @Param("status") DriverStatus status
    );
}