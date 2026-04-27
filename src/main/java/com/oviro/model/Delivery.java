package com.oviro.model;

import com.oviro.enums.DeliveryStatus;
import com.oviro.enums.PackageType;
import com.oviro.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "deliveries", indexes = {
    @Index(name = "idx_delivery_sender", columnList = "sender_id"),
    @Index(name = "idx_delivery_driver", columnList = "driver_id"),
    @Index(name = "idx_delivery_status", columnList = "status"),
    @Index(name = "idx_delivery_reference", columnList = "reference", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Delivery extends BaseEntity {

    @Column(name = "reference", unique = true, nullable = false, length = 20)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private DriverProfile driver;

    // Expéditeur
    @Column(name = "pickup_address", nullable = false, length = 500)
    private String pickupAddress;

    @Column(name = "pickup_latitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal pickupLatitude;

    @Column(name = "pickup_longitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal pickupLongitude;

    // Destinataire
    @Column(name = "dropoff_address", nullable = false, length = 500)
    private String dropoffAddress;

    @Column(name = "dropoff_latitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal dropoffLatitude;

    @Column(name = "dropoff_longitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal dropoffLongitude;

    @Column(name = "recipient_name", nullable = false, length = 200)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 20)
    private String recipientPhone;

    // Colis
    @Enumerated(EnumType.STRING)
    @Column(name = "package_type", nullable = false, length = 30)
    @Builder.Default
    private PackageType packageType = PackageType.OTHER;

    @Column(name = "package_description", length = 500)
    private String packageDescription;

    @Column(name = "package_weight_kg", precision = 6, scale = 2)
    private BigDecimal packageWeightKg;

    @Column(name = "fragile")
    @Builder.Default
    private boolean fragile = false;

    // Tarification
    @Column(name = "estimated_distance_km", precision = 8, scale = 3)
    private BigDecimal estimatedDistanceKm;

    @Column(name = "estimated_fare", precision = 12, scale = 2)
    private BigDecimal estimatedFare;

    @Column(name = "actual_fare", precision = 12, scale = 2)
    private BigDecimal actualFare;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.WALLET;

    // Statut
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.REQUESTED;

    @Column(name = "sender_note", length = 500)
    private String senderNote;

    // Confirmation de livraison
    @Column(name = "delivery_pin", length = 10)
    private String deliveryPin;

    @Column(name = "delivery_photo_url", length = 500)
    private String deliveryPhotoUrl;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "delivery_photo_data", columnDefinition = "LONGBLOB")
    private byte[] deliveryPhotoData;

    @Column(name = "delivery_photo_content_type", length = 100)
    private String deliveryPhotoContentType;

    // Timestamps
    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    // Notation
    @Column(name = "sender_rating")
    private Integer senderRating;

    @Column(name = "sender_review", length = 500)
    private String senderReview;
}
