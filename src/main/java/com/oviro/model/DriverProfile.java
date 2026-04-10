package com.oviro.model;

import com.oviro.enums.DriverStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "driver_profiles", indexes = {
        @Index(name = "idx_driver_license", columnList = "license_number", unique = true),
        @Index(name = "idx_driver_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    private PartnerProfile partner;

    @Column(name = "license_number", unique = true, nullable = false, length = 50)
    private String licenseNumber;

    @Column(name = "license_expiry_date")
    private java.time.LocalDate licenseExpiryDate;

    @Column(name = "national_id", length = 50)
    private String nationalId;

    @Column(name = "national_id_document_url")
    private String nationalIdDocumentUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private DriverStatus status = DriverStatus.OFFLINE;

    @Column(name = "current_latitude", precision = 10, scale = 7)
    private BigDecimal currentLatitude;

    @Column(name = "current_longitude", precision = 10, scale = 7)
    private BigDecimal currentLongitude;

    @Column(name = "rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = BigDecimal.valueOf(5.0);

    @Column(name = "total_rides")
    @Builder.Default
    private int totalRides = 0;

    @Column(name = "total_earnings", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalEarnings = BigDecimal.ZERO;

    @Column(name = "is_verified")
    @Builder.Default
    private boolean verified = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_vehicle_id")
    private Vehicle currentVehicle;

    @OneToMany(mappedBy = "driver", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Ride> rides = new ArrayList<>();

    // =========================
    // PHOTO DE PROFIL EN BLOB
    // =========================

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "profile_photo_data", columnDefinition = "LONGBLOB")
    private byte[] profilePhotoData;

    @Column(name = "profile_photo_content_type", length = 100)
    private String profilePhotoContentType;

    @Column(name = "profile_photo_file_name", length = 255)
    private String profilePhotoFileName;

    @Column(name = "profile_photo_size")
    private Long profilePhotoSize;
}