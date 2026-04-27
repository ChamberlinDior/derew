package com.oviro.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PricingZoneRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 300)
    private String description;

    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
    private BigDecimal centerLatitude;

    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
    private BigDecimal centerLongitude;

    @NotNull @DecimalMin("0.1")
    private BigDecimal radiusKm;

    @NotNull @DecimalMin("1.0") @DecimalMax("5.0")
    private BigDecimal surgeMultiplier;

    private BigDecimal baseFareOverride;

    private boolean active = true;
}
