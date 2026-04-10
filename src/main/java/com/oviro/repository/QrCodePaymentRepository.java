package com.oviro.repository;

import com.oviro.enums.QrCodeStatus;
import com.oviro.model.QrCodePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QrCodePaymentRepository extends JpaRepository<QrCodePayment, UUID> {

    Optional<QrCodePayment> findByToken(String token);

    Optional<QrCodePayment> findByRideId(UUID rideId);

    @Modifying
    @Query("UPDATE QrCodePayment q SET q.status = 'EXPIRED' WHERE q.expiresAt < :now AND q.status = 'ACTIVE'")
    int expireOldQrCodes(@Param("now") LocalDateTime now);

    boolean existsByRideIdAndStatus(UUID rideId, QrCodeStatus status);
}
