package com.oviro.dto.response;

import com.oviro.enums.PromoCodeType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PromoCodeResponse {
    private UUID id;
    private String code;
    private String description;
    private PromoCodeType type;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minRideAmount;
    private Integer maxUses;
    private int usesPerUser;
    private int totalUsed;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private boolean active;
    private BigDecimal discountAmount;
    private String message;
}
