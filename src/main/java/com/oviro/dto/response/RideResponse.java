package com.oviro.dto.response;

import com.oviro.enums.RideStatus;
import com.oviro.enums.ServiceType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class RideResponse {
    private UUID id;
    private String reference;
    private UUID clientId;
    private String clientName;
    private UUID driverId;
    private String driverName;
    private String driverPhone;
    private String pickupAddress;
    private BigDecimal pickupLatitude;
    private BigDecimal pickupLongitude;
    private String dropoffAddress;
    private BigDecimal dropoffLatitude;
    private BigDecimal dropoffLongitude;
    private BigDecimal estimatedDistanceKm;
    private Integer estimatedDurationMinutes;
    private BigDecimal estimatedFare;
    private BigDecimal actualFare;
    private RideStatus status;
    private ServiceType serviceType;
    private boolean forSomeoneElse;
    private String recipientName;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private Integer clientRating;
    private Integer driverRating;
    private String clientReview;
}
