package com.oviro.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ride_stops", indexes = {
    @Index(name = "idx_stop_ride", columnList = "ride_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideStop extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @Column(name = "stop_order", nullable = false)
    private int stopOrder;

    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Column(name = "latitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal longitude;

    @Column(name = "note", length = 200)
    private String note;

    @Column(name = "reached")
    @Builder.Default
    private boolean reached = false;
}
