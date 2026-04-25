package com.oviro.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "subscription_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    @Column(name = "included_trips", nullable = false)
    private int includedTrips;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "bonus_ut")
    @Builder.Default
    private int bonusUt = 0;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;
}
