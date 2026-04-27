package com.oviro.repository;

import com.oviro.model.PricingZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PricingZoneRepository extends JpaRepository<PricingZone, UUID> {

    List<PricingZone> findByActiveTrue();

    @Query(value = """
        SELECT * FROM pricing_zones z
        WHERE z.active = 1
        AND (6371 * ACOS(
            COS(RADIANS(:lat)) * COS(RADIANS(z.center_latitude))
            * COS(RADIANS(z.center_longitude) - RADIANS(:lng))
            + SIN(RADIANS(:lat)) * SIN(RADIANS(z.center_latitude))
        )) <= z.radius_km
        ORDER BY z.surge_multiplier DESC
        LIMIT 1
        """, nativeQuery = true)
    java.util.Optional<PricingZone> findZoneContaining(
            @Param("lat") BigDecimal lat,
            @Param("lng") BigDecimal lng);
}
