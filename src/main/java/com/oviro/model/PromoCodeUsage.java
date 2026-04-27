package com.oviro.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "promo_code_usages", indexes = {
    @Index(name = "idx_promo_usage_user", columnList = "user_id"),
    @Index(name = "idx_promo_usage_code", columnList = "promo_code_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoCodeUsage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_code_id", nullable = false)
    private PromoCode promoCode;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "ride_id")
    private UUID rideId;

    @Column(name = "delivery_id")
    private UUID deliveryId;

    @Column(name = "discount_applied", precision = 12, scale = 2, nullable = false)
    private BigDecimal discountApplied;
}
