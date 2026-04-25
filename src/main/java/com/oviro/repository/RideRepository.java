package com.oviro.repository;

import com.oviro.enums.RideStatus;
import com.oviro.model.Ride;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RideRepository extends JpaRepository<Ride, UUID> {

    Optional<Ride> findByReference(String reference);

    Page<Ride> findByClientId(UUID clientId, Pageable pageable);

    Page<Ride> findByDriverId(UUID driverId, Pageable pageable);

    Page<Ride> findByStatus(RideStatus status, Pageable pageable);

    List<Ride> findByDriverIdAndStatusIn(UUID driverId, List<RideStatus> statuses);

    @Query("SELECT r FROM Ride r WHERE r.client.id = :clientId AND r.status IN :statuses")
    List<Ride> findActiveRidesByClient(@Param("clientId") UUID clientId,
                                       @Param("statuses") List<RideStatus> statuses);

    @Query("""
        SELECT r FROM Ride r
        WHERE r.createdAt BETWEEN :from AND :to
        ORDER BY r.createdAt DESC
        """)
    Page<Ride> findByDateRange(@Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to,
                                Pageable pageable);

    @Query("SELECT COUNT(r) FROM Ride r WHERE r.driver.id = :driverId AND r.status = 'COMPLETED'")
    long countCompletedByDriver(@Param("driverId") UUID driverId);

    boolean existsByReference(String reference);

    long countByStatusIn(List<RideStatus> statuses);
}
