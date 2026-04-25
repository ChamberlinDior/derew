package com.oviro.repository;

import com.oviro.enums.SubscriptionStatus;
import com.oviro.model.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UUID> {

    List<UserSubscription> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<UserSubscription> findByUserIdAndStatus(UUID userId, SubscriptionStatus status);

    @Query("""
            SELECT s FROM UserSubscription s
             WHERE s.status = 'ACTIVE'
               AND s.endDate < :now
            """)
    List<UserSubscription> findExpiredActive(@Param("now") LocalDateTime now);

    @Query("""
            SELECT s FROM UserSubscription s
             WHERE s.status = 'ACTIVE'
               AND s.endDate BETWEEN :start AND :end
            """)
    List<UserSubscription> findExpiringBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
