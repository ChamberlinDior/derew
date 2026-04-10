package com.oviro.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class RideRequest {

    @NotBlank(message = "L'adresse de prise en charge est obligatoire")
    private String pickupAddress;

    @NotNull(message = "La latitude de prise en charge est obligatoire")
    @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    private BigDecimal pickupLatitude;

    @NotNull(message = "La longitude de prise en charge est obligatoire")
    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    private BigDecimal pickupLongitude;

    @NotBlank(message = "L'adresse de destination est obligatoire")
    private String dropoffAddress;

    @NotNull(message = "La latitude de destination est obligatoire")
    @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
    private BigDecimal dropoffLatitude;

    @NotNull(message = "La longitude de destination est obligatoire")
    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
    private BigDecimal dropoffLongitude;
}
