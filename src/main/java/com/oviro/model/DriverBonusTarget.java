package com.oviro.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "driver_bonus_targets", indexes = {
    @Index(name = "idx_bonus_driver", columnList = "driver_id"),
    @Index(name = "idx_bonus_date", columnList = "target_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverBonusTarget extends BaseEntity {

    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "required_rides", nullable = false)
    private int requiredRides;

    @Column(name = "completed_rides")
    @Builder.Default
    private int completedRides = 0;

    @Column(name = "bonus_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal bonusAmount;

    @Column(name = "achieved")
    @Builder.Default
    private boolean achieved = false;

    @Column(name = "bonus_credited")
    @Builder.Default
    private boolean bonusCredited = false;

    @Column(name = "description", length = 300)
    private String description;
}
