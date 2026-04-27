package com.oviro.repository;

import com.oviro.model.Referral;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferralRepository extends JpaRepository<Referral, UUID> {

    Optional<Referral> findByReferredId(UUID referredId);

    Page<Referral> findByReferrerId(UUID referrerId, Pageable pageable);

    List<Referral> findByReferrerIdAndBonusCreditedFalse(UUID referrerId);

    int countByReferrerId(UUID referrerId);
}
