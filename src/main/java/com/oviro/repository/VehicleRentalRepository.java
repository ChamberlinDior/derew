package com.oviro.repository;

import com.oviro.model.VehicleRental;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import com.oviro.enums.RentalStatus;

@Repository
public interface VehicleRentalRepository extends JpaRepository<VehicleRental, UUID> {

    Page<VehicleRental> findByPassengerId(UUID passengerId, Pageable pageable);

    Page<VehicleRental> findByStatus(RentalStatus status, Pageable pageable);

    Optional<VehicleRental> findByIdAndPassengerId(UUID id, UUID passengerId);
}
