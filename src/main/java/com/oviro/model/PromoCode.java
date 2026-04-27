package com.oviro.model;

import com.oviro.enums.PromoCodeType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "promo_codes", indexes = {
    @Index(name = "idx_promo_code", columnList = "code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoCode extends BaseEntity {

    @Column(name = "code", unique = true, nullable = false, length = 30)
    private String code;

    @Column(name = "description", length = 300)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PromoCodeType type;

    @Column(name = "discount_value", precision = 12, scale = 2, nullable = false)
    private BigDecimal discountValue;

    @Column(name = "max_discount_amount", precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "min_ride_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal minRideAmount = BigDecimal.ZERO;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "uses_per_user")
    @Builder.Default
    private int usesPerUser = 1;

    @Column(name = "total_used")
    @Builder.Default
    private int totalUsed = 0;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    @Column(name = "active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "applies_to_service_types", length = 200)
    private String appliesToServiceTypes;

    @OneToMany(mappedBy = "promoCode", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PromoCodeUsage> usages = new ArrayList<>();

    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return active
                && now.isAfter(validFrom)
                && now.isBefore(validUntil)
                && (maxUses == null || totalUsed < maxUses);
    }
}
