package com.oviro.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "restaurant_profiles", indexes = {
    @Index(name = "idx_restaurant_owner", columnList = "owner_id"),
    @Index(name = "idx_restaurant_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantProfile extends BaseEntity {

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Column(name = "latitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal longitude;

    @Column(name = "cuisine_type", length = 100)
    private String cuisineType;

    @Column(name = "opening_time", length = 10)
    private String openingTime;

    @Column(name = "closing_time", length = 10)
    private String closingTime;

    @Column(name = "delivery_radius_km", precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal deliveryRadiusKm = BigDecimal.valueOf(5.0);

    @Column(name = "min_order_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal minOrderAmount = BigDecimal.valueOf(1000);

    @Column(name = "delivery_fee", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal deliveryFee = BigDecimal.valueOf(500);

    @Column(name = "average_prep_minutes")
    @Builder.Default
    private int averagePrepMinutes = 20;

    @Column(name = "rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = BigDecimal.valueOf(5.0);

    @Column(name = "total_orders")
    @Builder.Default
    private int totalOrders = 0;

    @Column(name = "active")
    @Builder.Default
    private boolean active = true;

    @Column(name = "verified")
    @Builder.Default
    private boolean verified = false;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MenuItem> menuItems = new ArrayList<>();
}
