package com.oviro.dto.response;

import com.oviro.enums.ServiceType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Builder
public class RideEstimateResponse {

    private ServiceType serviceType;
    private BigDecimal estimatedPrice;
    private int estimatedDurationMinutes;
    private BigDecimal estimatedDistanceKm;
    private Map<String, BigDecimal> priceBreakdown;
    private boolean peakHourSurcharge;
    private boolean nightSurcharge;
}
