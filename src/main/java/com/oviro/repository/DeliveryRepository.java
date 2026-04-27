package com.oviro.repository;

import com.oviro.enums.DeliveryStatus;
import com.oviro.model.Delivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    Page<Delivery> findBySenderId(UUID senderId, Pageable pageable);

    @Query("SELECT d FROM Delivery d WHERE d.driver.id = :driverId ORDER BY d.createdAt DESC")
    Page<Delivery> findByDriverId(@Param("driverId") UUID driverId, Pageable pageable);

    Page<Delivery> findByStatus(DeliveryStatus status, Pageable pageable);

    @Query("SELECT d FROM Delivery d WHERE d.sender.id = :senderId AND d.status IN :statuses")
    List<Delivery> findActiveBySender(@Param("senderId") UUID senderId, @Param("statuses") List<DeliveryStatus> statuses);

    boolean existsByReference(String reference);
}
