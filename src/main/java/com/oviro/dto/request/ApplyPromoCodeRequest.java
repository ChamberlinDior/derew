package com.oviro.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApplyPromoCodeRequest {

    @NotBlank
    private String code;

    private BigDecimal rideAmount;
}
