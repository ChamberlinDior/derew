package com.oviro.repository;

import com.oviro.model.PromoCodeUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PromoCodeUsageRepository extends JpaRepository<PromoCodeUsage, UUID> {

    int countByPromoCodeIdAndUserId(UUID promoCodeId, UUID userId);

    boolean existsByPromoCodeIdAndUserId(UUID promoCodeId, UUID userId);
}
