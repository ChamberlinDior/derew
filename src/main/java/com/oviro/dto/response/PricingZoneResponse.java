package com.oviro.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class PricingZoneResponse {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal centerLatitude;
    private BigDecimal centerLongitude;
    private BigDecimal radiusKm;
    private BigDecimal surgeMultiplier;
    private BigDecimal baseFareOverride;
    private boolean active;
}
