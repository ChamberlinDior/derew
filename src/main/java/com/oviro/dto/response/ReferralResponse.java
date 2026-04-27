package com.oviro.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ReferralResponse {
    private UUID id;
    private UUID referrerId;
    private UUID referredId;
    private String referralCode;
    private BigDecimal referrerBonus;
    private BigDecimal referredBonus;
    private boolean bonusCredited;
    private LocalDateTime creditedAt;
    private boolean firstRideCompleted;
    private LocalDateTime createdAt;
    private int totalReferrals;
    private String myReferralCode;
    private BigDecimal totalBonusEarned;
}
