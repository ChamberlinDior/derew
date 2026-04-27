package com.oviro.dto.request;

import com.oviro.enums.PromoCodeType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PromoCodeRequest {

    @NotBlank
    @Size(min = 4, max = 30)
    private String code;

    @Size(max = 300)
    private String description;

    @NotNull
    private PromoCodeType type;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal discountValue;

    @DecimalMin("0.0")
    private BigDecimal maxDiscountAmount;

    @DecimalMin("0.0")
    private BigDecimal minRideAmount;

    private Integer maxUses;

    @Min(1)
    private int usesPerUser = 1;

    @NotNull
    private LocalDateTime validFrom;

    @NotNull
    private LocalDateTime validUntil;
}
