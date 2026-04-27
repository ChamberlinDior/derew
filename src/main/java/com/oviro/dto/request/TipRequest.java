package com.oviro.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TipRequest {

    @NotNull
    @DecimalMin("100.0")
    private BigDecimal amount;
}
