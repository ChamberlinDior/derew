package com.oviro.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class DriverEarningsResponse {
    private BigDecimal todayEarnings;
    private BigDecimal weekEarnings;
    private BigDecimal monthEarnings;
    private BigDecimal totalEarnings;
    private int todayRides;
    private int weekRides;
    private int monthRides;
    private int totalRides;
    private BigDecimal averageRating;
    private BigDecimal todayBonus;
    private int bonusRidesRequired;
    private int bonusRidesCompleted;
    private LocalDate bonusDate;
}
