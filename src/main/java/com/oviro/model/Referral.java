package com.oviro.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "referrals", indexes = {
    @Index(name = "idx_referral_referrer", columnList = "referrer_id"),
    @Index(name = "idx_referral_referred", columnList = "referred_id"),
    @Index(name = "idx_referral_code", columnList = "referral_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Referral extends BaseEntity {

    @Column(name = "referrer_id", nullable = false)
    private UUID referrerId;

    @Column(name = "referred_id", nullable = false, unique = true)
    private UUID referredId;

    @Column(name = "referral_code", nullable = false, length = 20)
    private String referralCode;

    @Column(name = "referrer_bonus", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal referrerBonus = BigDecimal.valueOf(2000);

    @Column(name = "referred_bonus", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal referredBonus = BigDecimal.valueOf(1000);

    @Column(name = "bonus_credited")
    @Builder.Default
    private boolean bonusCredited = false;

    @Column(name = "credited_at")
    private LocalDateTime creditedAt;

    @Column(name = "first_ride_completed")
    @Builder.Default
    private boolean firstRideCompleted = false;
}
