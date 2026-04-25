package com.oviro.model;

import com.oviro.enums.AddressType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "saved_addresses", indexes = {
        @Index(name = "idx_saved_addresses_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedAddress extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Column(name = "address_name", length = 255)
    private String addressName;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    @Builder.Default
    private AddressType type = AddressType.OTHER;

    @Column(name = "is_default")
    @Builder.Default
    private boolean isDefault = false;
}
