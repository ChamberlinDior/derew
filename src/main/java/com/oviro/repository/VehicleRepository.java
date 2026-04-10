package com.oviro.repository;

import com.oviro.model.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    Optional<Vehicle> findByPlateNumber(String plateNumber);

    Page<Vehicle> findByOwnerId(UUID ownerId, Pageable pageable);

    boolean existsByPlateNumber(String plateNumber);
}
