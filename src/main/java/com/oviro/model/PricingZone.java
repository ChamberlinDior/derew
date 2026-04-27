package com.oviro.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "pricing_zones", indexes = {
    @Index(name = "idx_zone_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingZone extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 300)
    private String description;

    @Column(name = "center_latitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal centerLatitude;

    @Column(name = "center_longitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal centerLongitude;

    @Column(name = "radius_km", precision = 8, scale = 3, nullable = false)
    private BigDecimal radiusKm;

    @Column(name = "surge_multiplier", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal surgeMultiplier = BigDecimal.ONE;

    @Column(name = "base_fare_override", precision = 12, scale = 2)
    private BigDecimal baseFareOverride;

    @Column(name = "active")
    @Builder.Default
    private boolean active = true;
}
