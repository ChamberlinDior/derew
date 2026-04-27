package com.oviro.repository;

import com.oviro.model.RestaurantProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface RestaurantProfileRepository extends JpaRepository<RestaurantProfile, UUID> {

    Page<RestaurantProfile> findByActiveTrue(Pageable pageable);

    Page<RestaurantProfile> findByActiveTrueAndVerifiedTrue(Pageable pageable);

    List<RestaurantProfile> findByOwnerId(UUID ownerId);

    @Query(value = """
        SELECT * FROM restaurant_profiles r
        WHERE r.active = 1 AND r.verified = 1
        AND (6371 * ACOS(
            COS(RADIANS(:lat)) * COS(RADIANS(r.latitude))
            * COS(RADIANS(r.longitude) - RADIANS(:lng))
            + SIN(RADIANS(:lat)) * SIN(RADIANS(r.latitude))
        )) <= r.delivery_radius_km
        ORDER BY r.rating DESC
        """, nativeQuery = true)
    List<RestaurantProfile> findNearby(@Param("lat") BigDecimal lat, @Param("lng") BigDecimal lng);
}
