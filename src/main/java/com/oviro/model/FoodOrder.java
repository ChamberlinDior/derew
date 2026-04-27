package com.oviro.model;

import com.oviro.enums.OrderStatus;
import com.oviro.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "food_orders", indexes = {
    @Index(name = "idx_order_client", columnList = "client_id"),
    @Index(name = "idx_order_restaurant", columnList = "restaurant_id"),
    @Index(name = "idx_order_status", columnList = "status"),
    @Index(name = "idx_order_reference", columnList = "reference", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodOrder extends BaseEntity {

    @Column(name = "reference", unique = true, nullable = false, length = 20)
    private String reference;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private RestaurantProfile restaurant;

    @Column(name = "driver_id")
    private UUID driverId;

    // Livraison
    @Column(name = "delivery_address", nullable = false, length = 500)
    private String deliveryAddress;

    @Column(name = "delivery_latitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal deliveryLatitude;

    @Column(name = "delivery_longitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal deliveryLongitude;

    // Tarification
    @Column(name = "items_total", precision = 12, scale = 2, nullable = false)
    private BigDecimal itemsTotal;

    @Column(name = "delivery_fee", precision = 12, scale = 2)
    private BigDecimal deliveryFee;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.WALLET;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "special_instructions", length = 500)
    private String specialInstructions;

    @Column(name = "estimated_delivery_minutes")
    private Integer estimatedDeliveryMinutes;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "ready_at")
    private LocalDateTime readyAt;

    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 300)
    private String cancellationReason;

    @Column(name = "client_rating")
    private Integer clientRating;

    @Column(name = "client_review", length = 500)
    private String clientReview;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<FoodOrderItem> items = new ArrayList<>();
}
