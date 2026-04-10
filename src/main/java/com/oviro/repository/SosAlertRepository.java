package com.oviro.repository;

import com.oviro.model.SosAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SosAlertRepository extends JpaRepository<SosAlert, UUID> {

    Page<SosAlert> findByResolvedFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<SosAlert> findByDriverId(UUID driverId, Pageable pageable);

    long countByResolvedFalse();
}
