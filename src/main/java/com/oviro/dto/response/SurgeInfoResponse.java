package com.oviro.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SurgeInfoResponse {
    private boolean surgeActive;
    private BigDecimal surgeMultiplier;
    private String zoneName;
    private String message;
}
