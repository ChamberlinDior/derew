package com.oviro.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ClientSosRequest {

    @NotNull
    @DecimalMin("-90.0") @DecimalMax("90.0")
    private BigDecimal latitude;

    @NotNull
    @DecimalMin("-180.0") @DecimalMax("180.0")
    private BigDecimal longitude;

    private String description;

    private UUID rideId;
}
