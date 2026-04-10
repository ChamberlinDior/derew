package com.oviro.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "partner_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "company_registration_number", length = 100)
    private String companyRegistrationNumber;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "commission_rate", precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal commissionRate = BigDecimal.valueOf(0.15);

    @Column(name = "is_verified")
    @Builder.Default
    private boolean verified = false;

    @OneToMany(mappedBy = "partner", fetch = FetchType.LAZY)
    @Builder.Default
    private List<DriverProfile> drivers = new ArrayList<>();

    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Vehicle> vehicles = new ArrayList<>();
}
