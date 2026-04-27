package com.oviro.dto.response;

import com.oviro.enums.DeliveryStatus;
import com.oviro.enums.PackageType;
import com.oviro.enums.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DeliveryResponse {
    private UUID id;
    private String reference;
    private UUID senderId;
    private String senderName;
    private UUID driverId;
    private String driverName;
    private String driverPhone;
    private String pickupAddress;
    private BigDecimal pickupLatitude;
    private BigDecimal pickupLongitude;
    private String dropoffAddress;
    private BigDecimal dropoffLatitude;
    private BigDecimal dropoffLongitude;
    private String recipientName;
    private String recipientPhone;
    private PackageType packageType;
    private String packageDescription;
    private BigDecimal packageWeightKg;
    private boolean fragile;
    private BigDecimal estimatedDistanceKm;
    private BigDecimal estimatedFare;
    private BigDecimal actualFare;
    private PaymentMethod paymentMethod;
    private DeliveryStatus status;
    private String senderNote;
    private String deliveryPin;
    private LocalDateTime requestedAt;
    private LocalDateTime assignedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private Integer senderRating;
}
