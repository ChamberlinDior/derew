package com.oviro.dto.response;

import com.oviro.enums.ServiceType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class NearbyDriverResponse {
    private UUID driverId;
    private String driverName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal rating;
    private String vehicleType;
    private String vehicleMake;
    private String vehicleModel;
    private String vehicleColor;
    private String plateNumber;
    private Double distanceKm;
    private int estimatedMinutes;
    private ServiceType serviceType;
}
