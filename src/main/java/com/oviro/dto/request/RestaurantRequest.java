package com.oviro.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RestaurantRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 500)
    private String description;

    @Size(max = 20)
    private String phone;

    @NotBlank
    @Size(max = 500)
    private String address;

    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
    private BigDecimal latitude;

    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
    private BigDecimal longitude;

    @Size(max = 100)
    private String cuisineType;

    private String openingTime;
    private String closingTime;

    @DecimalMin("0.5")
    private BigDecimal deliveryRadiusKm;

    @DecimalMin("0.0")
    private BigDecimal minOrderAmount;

    @DecimalMin("0.0")
    private BigDecimal deliveryFee;

    @Min(5) @Max(120)
    private int averagePrepMinutes = 20;
}
