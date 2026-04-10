package com.oviro.repository;

import com.oviro.model.PartnerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PartnerProfileRepository extends JpaRepository<PartnerProfile, UUID> {

    Optional<PartnerProfile> findByUserId(UUID userId);

    boolean existsByCompanyRegistrationNumber(String regNumber);
}
