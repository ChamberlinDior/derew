package com.oviro.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sos_alerts", indexes = {
    @Index(name = "idx_sos_driver", columnList = "driver_id"),
    @Index(name = "idx_sos_resolved", columnList = "resolved")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SosAlert extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private DriverProfile driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id")
    private Ride ride;

    @Column(name = "latitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal longitude;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "resolved")
    @Builder.Default
    private boolean resolved = false;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by_admin_id")
    private java.util.UUID resolvedByAdminId;

    @Column(name = "resolution_notes", length = 1000)
    private String resolutionNotes;
}
