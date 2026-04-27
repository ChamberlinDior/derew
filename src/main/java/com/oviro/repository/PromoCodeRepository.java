package com.oviro.repository;

import com.oviro.model.PromoCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PromoCodeRepository extends JpaRepository<PromoCode, UUID> {

    Optional<PromoCode> findByCode(String code);

    @Query("SELECT p FROM PromoCode p WHERE p.active = true AND p.validFrom <= :now AND p.validUntil >= :now")
    Page<PromoCode> findActivePromos(@Param("now") LocalDateTime now, Pageable pageable);
}
