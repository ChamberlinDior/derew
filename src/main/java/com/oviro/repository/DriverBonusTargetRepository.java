package com.oviro.repository;

import com.oviro.model.DriverBonusTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverBonusTargetRepository extends JpaRepository<DriverBonusTarget, UUID> {

    List<DriverBonusTarget> findByDriverId(UUID driverId);

    Optional<DriverBonusTarget> findByDriverIdAndTargetDate(UUID driverId, LocalDate date);

    List<DriverBonusTarget> findByDriverIdAndAchievedFalse(UUID driverId);
}
