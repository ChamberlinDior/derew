package com.oviro.model;

import com.oviro.enums.VehicleStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehicles", indexes = {
    @Index(name = "idx_vehicle_plate", columnList = "plate_number", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private PartnerProfile owner;

    @Column(name = "plate_number", unique = true, nullable = false, length = 20)
    private String plateNumber;

    @Column(name = "make", nullable = false, length = 100)
    private String make;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "color", length = 50)
    private String color;

    @Column(name = "type", length = 50)
    private String type; // MOTO, CAR, VAN, etc.

    @Column(name = "seats")
    private int seats;

    @Column(name = "insurance_document_url")
    private String insuranceDocumentUrl;

    @Column(name = "insurance_expiry_date")
    private java.time.LocalDate insuranceExpiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private VehicleStatus status = VehicleStatus.ACTIVE;
}
