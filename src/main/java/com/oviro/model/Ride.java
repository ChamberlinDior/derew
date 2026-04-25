package com.oviro.model;

import com.oviro.enums.RideStatus;
import com.oviro.enums.ServiceType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "rides", indexes = {
    @Index(name = "idx_ride_client", columnList = "client_id"),
    @Index(name = "idx_ride_driver", columnList = "driver_id"),
    @Index(name = "idx_ride_status", columnList = "status"),
    @Index(name = "idx_ride_reference", columnList = "reference", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ride extends BaseEntity {

    @Column(name = "reference", unique = true, nullable = false, length = 20)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private DriverProfile driver;

    // -- Origine --
    @Column(name = "pickup_address", nullable = false, length = 500)
    private String pickupAddress;

    @Column(name = "pickup_latitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal pickupLatitude;

    @Column(name = "pickup_longitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal pickupLongitude;

    // -- Destination --
    @Column(name = "dropoff_address", nullable = false, length = 500)
    private String dropoffAddress;

    @Column(name = "dropoff_latitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal dropoffLatitude;

    @Column(name = "dropoff_longitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal dropoffLongitude;

    // -- Tarification --
    @Column(name = "estimated_distance_km", precision = 8, scale = 3)
    private BigDecimal estimatedDistanceKm;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "estimated_fare", precision = 12, scale = 2)
    private BigDecimal estimatedFare;

    @Column(name = "actual_fare", precision = 12, scale = 2)
    private BigDecimal actualFare;

    @Column(name = "commission_amount", precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    // -- Cycle de vie --
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private RideStatus status = RideStatus.REQUESTED;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "pickup_at")
    private LocalDateTime pickupAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    // -- Notation --
    @Column(name = "client_rating")
    private Integer clientRating;

    @Column(name = "driver_rating")
    private Integer driverRating;

    @Column(name = "client_review", length = 1000)
    private String clientReview;

    // -- Type de service --
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", length = 20)
    @Builder.Default
    private ServiceType serviceType = ServiceType.STANDARD;

    // -- Pour quelqu'un d'autre --
    @Column(name = "for_someone_else", nullable = false)
    @Builder.Default
    private Boolean forSomeoneElse = false;

    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    @Column(name = "recipient_name", length = 200)
    private String recipientName;

    @Column(name = "sender_note", length = 500)
    private String senderNote;

    // -- Paiement --
    @OneToOne(mappedBy = "ride", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private QrCodePayment qrCodePayment;

    @OneToOne(mappedBy = "ride", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Transaction transaction;

    public boolean isForSomeoneElse() {
        return Boolean.TRUE.equals(forSomeoneElse);
    }
}
